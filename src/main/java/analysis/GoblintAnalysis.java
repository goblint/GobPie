package analysis;

import com.ibm.wala.classLoader.Module;
import api.GoblintService;
import api.messages.GoblintAnalysisResult;
import api.messages.GoblintFunctionsResult;
import api.messages.GoblintMessagesResult;
import api.messages.Params;
import goblintserver.GoblintServer;
import gobpie.GobPieConfiguration;
import gobpie.GobPieException;
import gobpie.GobPieExceptionType;
import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.process.UnixProcess;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
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
 * @since 0.0.1
 */

public class GoblintAnalysis implements ServerAnalysis {

    private final MagpieServer magpieServer;
    private final GoblintServer goblintServer;
    private final GoblintService goblintService;
    private final GobPieConfiguration gobpieConfiguration;
    private final FileAlterationObserver goblintConfObserver;
    private final int SIGINT = 2;
    private static Future<?> lastAnalysisTask;
    private static final ExecutorService execService = Executors.newSingleThreadExecutor();
    private final AtomicBoolean analysisRunning = new AtomicBoolean(false);

    private final Logger log = LogManager.getLogger(GoblintAnalysis.class);


    public GoblintAnalysis(MagpieServer magpieServer, GoblintServer goblintServer, GoblintService goblintService, GobPieConfiguration gobpieConfiguration) {
        this.magpieServer = magpieServer;
        this.goblintServer = goblintServer;
        this.goblintService = goblintService;
        this.gobpieConfiguration = gobpieConfiguration;
        this.goblintConfObserver = createGoblintConfObserver();
    }


    /**
     * The source of this analysis, usually the name of the analysis.
     *
     * @return the string
     */

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
        if (consumer instanceof MagpieServer server) {
            if (analysisRunning.get()) {
                abortAnalysis();
            }
            goblintConfObserver.checkAndNotify();
            preAnalyse();
            log.info("---------------------- Analysis started ----------------------");
            Runnable analysisTask = () -> {
                Collection<AnalysisResult> response = reanalyse();
                if (response != null) {
                    server.consume(new ArrayList<>(response), source());
                    log.info("--------------------- Analysis finished ----------------------");
                }
            };
            lastAnalysisTask = execService.submit(analysisTask);
        }
    }


    /**
     * The method that is triggered before each analysis.
     * <p>
     * preAnalyzeCommand is read from the GobPie configuration file.
     * Can be used for automating the compilation database generation.
     */

    private void preAnalyse() {
        String[] preAnalyzeCommand = gobpieConfiguration.getPreAnalyzeCommand();
        if (preAnalyzeCommand != null) {
            try {
                log.info("Preanalyze command ran: \"" + Arrays.toString(preAnalyzeCommand) + "\"");
                runCommand(new File(System.getProperty("user.dir")), preAnalyzeCommand);
                log.info("Preanalyze command finished.");
            } catch (IOException | InvalidExitValueException | InterruptedException | TimeoutException e) {
                this.magpieServer.forwardMessageToClient(
                        new MessageParams(MessageType.Warning, "Running preanalysis command failed. " + e.getMessage()));
            }
        }
    }


    private void abortAnalysis() {
        if (lastAnalysisTask != null && !lastAnalysisTask.isDone()) {
            Process goblintProcess = goblintServer.getGoblintRunProcess().getProcess();
            int pid = Math.toIntExact(goblintProcess.pid());
            UnixProcess unixProcess = new UnixProcess(pid);
            try {
                unixProcess.kill(SIGINT);
                log.info("--------------- This analysis has been aborted -------------");
            } catch (IOException e) {
                log.error("Aborting analysis failed.");
            }
        }

    }


    /**
     * Sends the requests to Goblint server and gets their results.
     *
     * @return a collection of warning messages and cfg code lenses if request was successful, null otherwise.
     */

    private Collection<AnalysisResult> reanalyse() {

        try {
            // Analyze
            analysisRunning.set(true);
            GoblintAnalysisResult analyzeResult = goblintService.analyze(new Params()).get();
            analysisRunning.set(false);
            if (analyzeResult.getStatus().contains("Aborted"))
                return null;
            // Get warning messages
            List<GoblintMessagesResult> messages = goblintService.messages().get();
            // Get list of functions
            List<GoblintFunctionsResult> functions = goblintService.functions().get();
            return Stream.concat(convertMessagesFromJson(messages).stream(), convertFunctionsFromJson(functions).stream()).collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException e) {
            throw new GobPieException("Sending the request to or receiving result from the server failed.", e, GobPieExceptionType.GOBLINT_EXCEPTION);
        }
    }


    /**
     * Method for running a command.
     *
     * @param dirPath The directory in which the command will run.
     * @param command The command to run.
     * @return Exit value and output of a finished process.
     */

    public ProcessResult runCommand(File dirPath, String[] command) throws IOException, InvalidExitValueException, InterruptedException, TimeoutException {
        log.debug("Waiting for command: " + Arrays.toString(command) + " to run...");
        return new ProcessExecutor()
                .directory(dirPath)
                .command(command)
                .redirectOutput(System.err)
                .redirectError(System.err)
                .execute();
    }


    /**
     * Deserializes json from the response and converts the information
     * into AnalysisResult objects, which Magpie uses to generate IDE messages.
     *
     * @param response that was read from the socket and needs to be converted to AnalysisResults.
     * @return A collection of AnalysisResult objects.
     */

    private Collection<AnalysisResult> convertMessagesFromJson(List<GoblintMessagesResult> response) {
        return response.stream().map(GoblintMessagesResult::convert).flatMap(List::stream).collect(Collectors.toList());
    }

    private Collection<AnalysisResult> convertFunctionsFromJson(List<GoblintFunctionsResult> response) {
        return response.stream().map(GoblintFunctionsResult::convert).collect(Collectors.toList());
    }


    /**
     * Method for creating an observer for Goblint configuration file.
     * So that the server could be restarted when the configuration file is changed.
     * TODO: instead of restarting the server, send new configuration with a request to the server #32
     *
     * @return The FileAlterationObserver of project root directory.
     */

    public FileAlterationObserver createGoblintConfObserver() {

        FileFilter fileFilter = file -> file.getName().equals(gobpieConfiguration.getGoblintConf());

        FileAlterationObserver observer = new FileAlterationObserver(System.getProperty("user.dir"), fileFilter);
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileChange(File file) {
                try {
                    goblintServer.restartGoblintServer();
                    // TODO: doesn't work (does not connect to new socket)
                } catch (GobPieException e) {
                    String message = "Unable to restart GobPie extension: " + e.getMessage();
                    magpieServer.forwardMessageToClient(
                            new MessageParams(MessageType.Error, message + " Please check the output terminal of GobPie extension for more information.")
                    );
                    if (e.getCause() == null) log.error(message);
                    else log.error(message + " Cause: " + e.getCause().getMessage());
                }
            }
        });

        try {
            observer.initialize();
        } catch (Exception e) {
            this.magpieServer.forwardMessageToClient(
                    new MessageParams(MessageType.Warning, "After changing the files list in Goblint configuration the server will not be automatically restarted. Close and reopen the IDE to restart the server manually if needed."));
            log.error("Initializing goblintConfObserver failed: " + e.getMessage());
        }
        return observer;
    }


}
