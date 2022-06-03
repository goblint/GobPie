package analysis;

import com.ibm.wala.classLoader.Module;
import goblintclient.GoblintClient;
import goblintclient.communication.AnalyzeResponse;
import goblintclient.communication.MessagesResponse;
import goblintclient.communication.Request;
import goblintclient.messages.GoblintMessages;
import goblintserver.GoblintServer;
import gobpie.GobPieConfiguration;
import gobpie.GobPieException;
import gobpie.GobPieExceptionType;
import magpiebridge.core.AnalysisConsumer;
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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;


public class GoblintAnalysis implements ServerAnalysis {

    private final MagpieServer magpieServer;
    private final GoblintServer goblintServer;
    private final GoblintClient goblintClient;
    private final GobPieConfiguration gobpieConfiguration;
    private final FileAlterationObserver goblintConfObserver;
    private final int SIGINT = 2;

    private Boolean analysisRunning = false;

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

                if (analysisRunning) { 
                    abortAnalysis();
                }
                goblintConfObserver.checkAndNotify();
                preAnalyse();

                log.info("\n---------------------- Analysis started ----------------------");
                analysisRunning = true;
                Collection<GoblintAnalysisResult> response = reanalyse();
                if (response != null) {
                    server.consume(new ArrayList<>(response), source());
                    analysisRunning = false;
                    log.info("--------------------- Analysis finished ----------------------\n");
                }
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


    private void abortAnalysis() {
        Process goblintProcess = goblintServer.getGoblintRunProcess().getProcess();
        int pid = Math.toIntExact(goblintProcess.pid());
        UnixProcess unixProcess = new UnixProcess(pid);
        try {
            unixProcess.kill(SIGINT);
            log.info("--------------- This analysis has been aborted -------------\n");
        } catch (IOException e) {
            log.error("Aborting analysis failed.");
        }
    }


    /**
     * Sends the request to Goblint server to reanalyse and reads the result.
     *
     * @return returns a collection of analysis results if the request was sucessful, null otherwise
     */

    private Collection<GoblintAnalysisResult> reanalyse() {

        Request analyzeRequest = new Request("analyze");
        Request messagesRequest = new Request("messages");

        try {
            goblintClient.writeRequestToSocket(analyzeRequest);
            AnalyzeResponse analyzeResponse = goblintClient.readAnalyzeResponseFromSocket();
            if (!analyzeRequest.getId().equals(analyzeResponse.getId()))
                throw new GobPieException("Response ID does not match request ID.", GobPieExceptionType.GOBLINT_EXCEPTION);
            if (analyzeResponse.getResult().getStatus().contains("Aborted"))
                return null;
            goblintClient.writeRequestToSocket(messagesRequest);
            MessagesResponse messagesResponse = goblintClient.readMessagesResponseFromSocket();
            if (!messagesRequest.getId().equals(messagesResponse.getId()))
                throw new GobPieException("Response ID does not match request ID.", GobPieExceptionType.GOBLINT_EXCEPTION);
            return convertResultsFromJson(messagesResponse);
        } catch (IOException e) {
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
     * Deserializes json to GoblintResult objects and then converts the information
     * into GoblintAnalysisResult objects, which Magpie uses to generate IDE
     * messages.
     *
     * @return A collection of GoblintAnalysisResult objects.
     */

    private Collection<GoblintAnalysisResult> convertResultsFromJson(MessagesResponse messagesResponse) {

        Collection<GoblintAnalysisResult> results = new ArrayList<>();
        try {
            List<GoblintMessages> messagesArray = messagesResponse.getResult();
            for (GoblintMessages msg : messagesArray) {
                results.addAll(msg.convert());
            }
            return results;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Method for creating an observer for Goblint configuration file.
     * So that the server could be restarted when the configuration file is changed.
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
