import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import com.ibm.wala.classLoader.Module;

import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.MagpieServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;


public class GoblintAnalysis implements ServerAnalysis {

    private final MagpieServer magpieServer;
    private File jsonResult = new File("analysisResults.json");
    private File gobPieConf = new File("gobpie.json");
    private String pathToGoblintConf;
    private String[] filesToAnalyze;
    private String[] preAnalyzeCommand;
    private String[] goblintRunCommand;
    // private List<String> projectFiles; // for future use

    private final Logger log;

    public GoblintAnalysis(MagpieServer server) {
        this.magpieServer = server;
        this.log = LogManager.getLogger(GoblintAnalysis.class);
    }

    /**
     * The source of this analysis, usually the name of the analysis.
     *
     * @return the string
     */
    public String source() {
        return "GoblintAnalysis";
    }


    /**
     * The files to be analyzed.
     *
     * @param files    the files that have been opened in the editor (not using due to using the compilation database).
     * @param consumer the server which consumes the analysis results.
     * @param rerun    tells if the analysis should be reran.
     */
    @Override
    public void analyze(Collection<? extends Module> files, AnalysisConsumer consumer, boolean rerun) {
        if (rerun) {
            if (consumer instanceof MagpieServer) {
                boolean gobpieconf = readGobPieConfiguration();
                if (!gobpieconf) return;
                if (preAnalyzeCommand != null && preAnalyzeCommand.length > 0) {
                    try { 
                        runCommand(new File(System.getProperty("user.dir")), preAnalyzeCommand);
                    } catch (IOException | InvalidExitValueException | InterruptedException | TimeoutException e) {
                        this.magpieServer.forwardMessageToClient(new MessageParams(MessageType.Warning, "Building compilation database failed. " + e.getMessage()));
                    }
                }
                log.info("New analysis started");
                MagpieServer server = (MagpieServer) consumer;
                if (generateJson()) server.consume(new ArrayList<>(readResultsFromJson()), source());
            }
        }
    }


    public ProcessResult runCommand(File dirPath, String[] command) throws IOException, InvalidExitValueException, InterruptedException, TimeoutException {
        log.debug("Waiting for Goblint to run...");
        System.err.println("---------------------- Goblint's dump start ----------------------");
        ProcessResult process = new ProcessExecutor()
                .directory(dirPath)
                .command(command)
                .redirectOutput(System.err)
                .redirectError(System.err)
                .execute();
        System.err.println("----------------------- Goblint's dump end -----------------------");
        return process;
    }

    /**
     * Runs the command in the project root directory
     * to let goblint generate the json file with analysis results.
     *
     * @param file the file on which to run the analysis.
     * @return returns true if goblint finished the analysis and json was generated sucessfully, false otherwise
     */
    private boolean generateJson() {
        // construct command to run
        this.goblintRunCommand = Stream.concat(
                                    Arrays.stream(new String[]{"goblint", "--conf", pathToGoblintConf, "--set", "result", "json-messages", "-o", jsonResult.getAbsolutePath()}), 
                                    Arrays.stream(filesToAnalyze))
                                    .toArray(String[]::new);

        try {
            // run command
            log.info("Goblint run with command: " + String.join(" ", goblintRunCommand));
            ProcessResult commandRunProcess = runCommand(new File(System.getProperty("user.dir")), goblintRunCommand);
            if (commandRunProcess.getExitValue() != 0) {
                magpieServer.forwardMessageToClient(
                        new MessageParams(MessageType.Error,
                                "Goblint exited with an error."));
                log.error("Goblint exited with an error.");
                return false;
            }
            log.info("Goblint finished analyzing.");
            return true;
        } catch (IOException | InvalidExitValueException | InterruptedException | TimeoutException e) {
            this.magpieServer.forwardMessageToClient(new MessageParams(MessageType.Error, "Running Goblint failed. " + e.getMessage()));
            return false;
        }
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
            JsonObject json = JsonParser.parseReader(new FileReader(jsonResult)).getAsJsonObject();
            GsonBuilder builder = new GsonBuilder();
            // Add deserializer for tags
            builder.registerTypeAdapter(GoblintResult.Message.tag.class, new TagInterfaceAdapter());
            Gson gson = builder.create();
            GoblintResult goblintResult = gson.fromJson(json, GoblintResult.class);
            Collection<GoblintAnalysisResult> results = goblintResult.convert();
            // this.projectFiles = goblintResult.getFiles();
            log.debug("Analysis results read from json");
            return results;
        } catch (JsonIOException | JsonSyntaxException | FileNotFoundException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean readGobPieConfiguration() {
        try {
            log.debug("Reading GobPie configuration from json");
            Gson gson = new GsonBuilder().create();
            // Read json object
            JsonObject jsonObject = JsonParser.parseReader(new FileReader(gobPieConf)).getAsJsonObject();
            // Convert json object to GobPieConfiguration object
            GobPieConfiguration gobpieConfiguration = gson.fromJson(jsonObject, GobPieConfiguration.class);
            this.pathToGoblintConf = new File(gobpieConfiguration.getGoblintConf()).getAbsolutePath().toString();
            this.filesToAnalyze = gobpieConfiguration.getFiles();
            this.preAnalyzeCommand = gobpieConfiguration.getPreAnalyzeCommand();
            if (gobpieConfiguration.getGoblintConf().equals("") || gobpieConfiguration.getFiles() == null || gobpieConfiguration.getFiles().length < 1) {
                log.debug("Configuration parameters missing from GobPie configuration file");
                magpieServer.forwardMessageToClient(new MessageParams(MessageType.Error, "Configuration parameters missing from GobPie configuration file."));
                return false;
            }
            log.debug("GobPie configuration read from json");
        } catch (JsonIOException | JsonSyntaxException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            this.magpieServer.forwardMessageToClient(new MessageParams(MessageType.Error, "Could not locate GobPie configuration file. " + e.getMessage()));
            return false;
        }
        return true;
    }

}
