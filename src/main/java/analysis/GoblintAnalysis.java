package analysis;

import com.ibm.wala.classLoader.Module;
import goblintclient.GoblintClient;
import goblintclient.communication.*;
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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class GoblintAnalysis implements ServerAnalysis {

    private final MagpieServer magpieServer;
    private final GoblintServer goblintServer;
    private final GoblintClient goblintClient;
    private final GobPieConfiguration gobpieConfiguration;
    private final FileAlterationObserver goblintConfObserver;

    private final Logger log = LogManager.getLogger(GoblintAnalysis.class);


    public GoblintAnalysis(MagpieServer magpieServer, GoblintServer goblintServer, GoblintClient goblintClient, GobPieConfiguration gobpieConfiguration) {
        this.magpieServer = magpieServer;
        this.goblintServer = goblintServer;
        this.goblintClient = goblintClient;
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
     * @param rerun    tells if the analysis should be reran.
     */

    @Override
    public void analyze(Collection<? extends Module> files, AnalysisConsumer consumer, boolean rerun) {
        if (rerun) {
            if (consumer instanceof MagpieServer server) {

                goblintConfObserver.checkAndNotify();
                preAnalyse();

                System.err.println("\n---------------------- Analysis started ----------------------");
                Collection<AnalysisResult> response = reanalyse();
                if (response != null) server.consume(new ArrayList<>(response), source());
                System.err.println("--------------------- Analysis finished ----------------------\n");

            }
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


    /**
     * Sends the requests to Goblint server and reads their results.
     *
     * @return a collection of warning messages and cfg code lenses if request was successful, null otherwise.
     */

    private Collection<AnalysisResult> reanalyse() {

        Request analyzeRequest = new Request("analyze");
        Request messagesRequest = new Request("messages");
        Request functionsRequest = new Request("functions");

        try {
            // Analyze
            getResponse(analyzeRequest);
            // Get warning messages
            MessagesResponse messagesResponse = (MessagesResponse) getResponse(messagesRequest);
            // Get list of functions
            FunctionsResponse functionsResponse = (FunctionsResponse) getResponse(functionsRequest);
            return Stream.concat(convertResultsFromJson(messagesResponse).stream(), convertResultsFromJson(functionsResponse).stream()).collect(Collectors.toList());
        } catch (IOException e) {
            log.info("Sending the request to or receiving result from the server failed: " + e);
            return null;
        }
    }

    /**
     * Writes the request to the socket and reads its response according to the request that was sent.
     *
     * @param request The request to be written into socket.
     * @return the response to the request that was sent.
     * @throws GobPieException if the request and response ID do not match.
     */

    private Response getResponse(Request request) throws IOException {
        goblintClient.writeRequestToSocket(request);
        Response response;
        if (request.getMethod().equals("analyze"))
            response = goblintClient.readAnalyzeResponseFromSocket();
        else if (request.getMethod().equals("messages"))
            response = goblintClient.readMessagesResponseFromSocket();
        else
            response = goblintClient.readFunctionsResponseFromSocket();
        if (!request.getId().equals(response.getId()))
            throw new GobPieException("Response ID does not match request ID.", GobPieExceptionType.GOBLINT_EXCEPTION);
        return response;
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

    private Collection<AnalysisResult> convertResultsFromJson(MessagesResponse response) {
        return response.getResult().stream().map(msg -> {
            try {
                return msg.convert();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).flatMap(List::stream).collect(Collectors.toList());
    }

    private Collection<AnalysisResult> convertResultsFromJson(FunctionsResponse response) {
        return response.getResult().stream().map(msg -> {
            try {
                return msg.convert();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).flatMap(List::stream).collect(Collectors.toList());
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
                    goblintClient.connectGoblintClient();
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
