package analysis;

import api.GoblintService;
import api.messages.GoblintAnalysisResult;
import api.messages.GoblintFunctionsResult;
import api.messages.GoblintMessagesResult;
import api.messages.params.AnalyzeParams;
import com.ibm.wala.classLoader.Module;
import goblintserver.GoblintConfWatcher;
import goblintserver.GoblintServer;
import gobpie.GobPieConfiguration;
import gobpie.GobPieException;
import gobpie.GobPieExceptionType;
import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

/**
 * The Class GoblintAnalysis.
 * <p>
 * Implementation of the ServerAnalysis interface.
 * The class that is responsible for analyzing when an analysis event is triggered.
 * Sends the requests to Goblint server, reads the corresponding responses,
 * converts the results and passes them to MagpieBridge server.
 *
 * @author Karoliine Holter
 * @author Juhan Oskar Hennoste
 * @since 0.0.1
 */

public class GoblintAnalysis implements ServerAnalysis {

    private final MagpieServer magpieServer;
    private final GoblintServer goblintServer;
    private final GoblintService goblintService;
    private final GobPieConfiguration gobpieConfiguration;
    private final GoblintConfWatcher goblintConfWatcher;
    private static Future<?> lastAnalysisTask = null;

    private final Logger log = LogManager.getLogger(GoblintAnalysis.class);



    public GoblintAnalysis(MagpieServer magpieServer, GoblintServer goblintServer, GoblintService goblintService, GobPieConfiguration gobpieConfiguration, GoblintConfWatcher goblintConfWatcher) {
        this.magpieServer = magpieServer;
        this.goblintServer = goblintServer;
        this.goblintService = goblintService;
        this.gobpieConfiguration = gobpieConfiguration;
        this.goblintConfWatcher = goblintConfWatcher;
    }


    /**
     * The source of this analysis, usually the name of the analysis.
     *
     * @return the string
     */
    @Override
    public String source() {
        return "GobPie";
    }


    /**
     * The method that is triggered to start a new analysis.
     *
     * @param files    the files that have been opened in the editor (not using due to using the compilation database).
     * @param consumer the server which consumes the analysis results.
     * @param rerun    tells if the analysis should be rerun.
     */
    @Override
    public void analyze(Collection<? extends Module> files, AnalysisConsumer consumer, boolean rerun) {
        if (!rerun) {
            // According to MagpieBridge source code with doAnalysisByOpen = false and doAnalysisByFirstOpen = true,
            // rerun is true iff either the first file was opened or a file was saved i.e. exactly the cases where analysis should be performed.
            return;
        }

        if (!goblintServer.isAlive()) {
            // Goblint server has crashed. Exit GobPie because without the server no analysis is possible.
            magpieServer.exit();
            return;
        }

        if (lastAnalysisTask != null && !lastAnalysisTask.isDone()) {
            lastAnalysisTask.cancel(true);
            try {
                goblintServer.abortAnalysis();
                log.info("--------------- This analysis has been aborted -------------");
            } catch (IOException e) {
                log.error("Aborting analysis failed.");
            }
        }

        if (!goblintConfWatcher.refreshGoblintConfig()) {
            return;
        }

        magpieServer.forwardMessageToClient(new MessageParams(MessageType.Info, source() + " started analyzing the code."));
        goblintServer.preAnalyse();
        log.info("---------------------- Analysis started ----------------------");

        lastAnalysisTask = reanalyse().thenAccept(response -> {
            consumer.consume(new ArrayList<>(response), source());

            log.info("--------------------- Analysis finished ----------------------");
            magpieServer.forwardMessageToClient(new MessageParams(MessageType.Info, source() + " finished analyzing the code."));
        }).exceptionally(ex -> {

            Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
            // TODO: handle closed socket exceptions:
            //      org.eclipse.lsp4j.jsonrpc.JsonRpcException: java.net.SocketException: Broken pipe; errno=32
            //  and org.eclipse.lsp4j.jsonrpc.JsonRpcException: org.newsclub.net.unix.SocketClosedException: Not open
            log.error("--------------------- Analysis failed  ----------------------");
            log.error(cause);
            magpieServer.forwardMessageToClient(new MessageParams(MessageType.Error, source() + " failed to analyze the code:\n" + cause.getMessage()));
            return null;
        });
    }



    /**
     * Sends the requests to Goblint server and gets their results.
     * Checks if analysis succeeded.
     * If analysis succeeds, requests the messages from the Goblint server.
     * If showCfg option is turned on, asks for the function names for code lenses.
     *
     * @return a CompletableFuture of a collection of warning messages and cfg code lenses if request was successful.
     * @throws GobPieException in case the analysis was aborted or returned a VerifyError.
     */
    public CompletableFuture<Collection<AnalysisResult>> reanalyse() {
        return goblintService.analyze(new AnalyzeParams(!gobpieConfiguration.incrementalAnalysis()))
                .thenCompose(this::getComposedAnalysisResults);
    }

    private void didAnalysisNotSucceed(GoblintAnalysisResult analysisResult) {
        if (analysisResult.status().contains("Aborted"))
            throw new GobPieException("The running analysis has been aborted.", GobPieExceptionType.GOBLINT_EXCEPTION);
        else if (analysisResult.status().contains("VerifyError"))
            throw new GobPieException("Analysis returned VerifyError.", GobPieExceptionType.GOBLINT_EXCEPTION);
    }

    private CompletableFuture<Collection<AnalysisResult>> getComposedAnalysisResults(GoblintAnalysisResult analysisResult) {
        didAnalysisNotSucceed(analysisResult);
        // Get warning messages
        CompletableFuture<Collection<AnalysisResult>> messagesCompletableFuture = goblintService.messages()
                .thenApply(this::convertMessagesFromJson);
        if (!gobpieConfiguration.showCfg()) {
            return messagesCompletableFuture;
        }
        // Get list of functions
        CompletableFuture<Collection<AnalysisResult>> functionsCompletableFuture = goblintService.functions()
                .thenApply(this::convertFunctionsFromJson);
        return messagesCompletableFuture
                .thenCombine(functionsCompletableFuture, (messages, functions) -> Stream.concat(messages.stream(), functions.stream()).toList());
    }


    /**
     * Deserializes json from the response and converts the information
     * into AnalysisResult objects, which Magpie uses to generate IDE messages.
     *
     * @param response that was read from the socket and needs to be converted to AnalysisResults.
     * @return A collection of AnalysisResult objects.
     */

    private Collection<AnalysisResult> convertMessagesFromJson(List<GoblintMessagesResult> response) {
        return response.stream().map(msg -> msg.convert(gobpieConfiguration.explodeGroupWarnings())).flatMap(List::stream).toList();
    }

    private Collection<AnalysisResult> convertFunctionsFromJson(List<GoblintFunctionsResult> response) {
        return response.stream().map(GoblintFunctionsResult::convert).flatMap(List::stream).toList();
    }

}
