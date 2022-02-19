package analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.TimeoutException;

import com.ibm.wala.classLoader.Module;

import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.MagpieServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;

import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import goblintserver.GoblintClient;
import goblintserver.GoblintServer;
import goblintserver.Request;


public class GoblintAnalysis implements ServerAnalysis {

    private final MagpieServer magpieServer;
    private final GoblintServer goblintServer;
    private final GoblintClient goblintClient;

    private File jsonResult = new File("analysisResults.json");

    // private List<String> projectFiles; // for future use

    private final Logger log = LogManager.getLogger(GoblintAnalysis.class);


    public GoblintAnalysis(MagpieServer magpieServer, GoblintServer goblintServer, GoblintClient goblintClient) {
        this.magpieServer = magpieServer;
        this.goblintServer = goblintServer;
        this.goblintClient = goblintClient;
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
            if (consumer instanceof MagpieServer) {

                preAnalyse();

                System.err.println("\n---------------------- Analysis started ----------------------");
                MagpieServer server = (MagpieServer) consumer;
                if (reanalyse()) server.consume(new ArrayList<>(readResultsFromJson()), source());
                System.err.println("--------------------- Analysis finished ----------------------\n");

            }
        }
    }


    /**
     * The method that is triggered before each analysis.
     * 
     * preAnalyzeCommand is read from the GobPie configuration file.
     * Can be used for automating the compilation database generation.
     */

    private void preAnalyse() {
        String[] preAnalyzeCommand = goblintServer.getPreAnalyzeCommand();
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
     * Sends the request to Goblint server to reanalyse and reads the result.
     *
     * @return returns true if the request was sucessful, false otherwise
     */

    private boolean reanalyse() {

        // {"jsonrpc":"2.0","id":0,"method":"analyze","params":{}}
        String request1 = new GsonBuilder().create().toJson(new Request("analyze")) + "\n";
        String request2 = new GsonBuilder().create().toJson(new Request("messages")) + "\n";

        try {
            goblintClient.writeRequestToSocket(request1);
            goblintClient.readResultFromSocket();
            goblintClient.writeRequestToSocket(request2);
            goblintClient.readResultFromSocket();
            return true;
        } catch (IOException e) {
            log.info("Sending the request to or receiving result from the server failed: " + e);
            e.printStackTrace();
            return false;
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
        log.debug("Waiting for command: " + command.toString() + " to run...");
        ProcessResult process = new ProcessExecutor()
                .directory(dirPath)
                .command(command)
                .redirectOutput(System.err)
                .redirectError(System.err)
                .execute();
        return process;
    }


    /**
     * Deserializes json to GoblintResult objects and then converts the information
     * into GoblintAnalysisResult objects, which Magpie uses to generate IDE
     * messages.
     *
     * @return A collection of GoblintAnalysisResult objects.
     */

    private Collection<GoblintAnalysisResult> readResultsFromJson() {
        try {
            log.debug("Reading analysis results from json");
            // Read json objects as an array
            JsonElement json = JsonParser.parseReader(new FileReader(jsonResult));
            if (!json.isJsonObject()) {
                log.error("Reading analysis results failed.");
                this.magpieServer.forwardMessageToClient(
                        new MessageParams(MessageType.Error, "Reading analysis results failed."));
                return new ArrayList<GoblintAnalysisResult>();
            }
            GsonBuilder builder = new GsonBuilder();
            // Add deserializer for tags
            builder.registerTypeAdapter(GoblintResult.Message.tag.class, new TagInterfaceAdapter());
            Gson gson = builder.create();
            GoblintResult goblintResult = gson.fromJson(json.getAsJsonObject(), GoblintResult.class);
            Collection<GoblintAnalysisResult> results = goblintResult.convert();
            // this.projectFiles = goblintResult.getFiles();
            log.debug("Analysis results read from json");
            return results;
        } catch (JsonIOException | JsonSyntaxException | FileNotFoundException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


}
