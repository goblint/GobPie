import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.TimeoutException;

import com.ibm.wala.classLoader.Module;

import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.MagpieServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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

    final private MagpieServer magpieServer;
    private String pathToJsonResult = System.getProperty("user.dir") + "/" + "analysisResults.json";
    private String pathToGobPieConf = System.getProperty("user.dir") + "/" + "gobpie.json";
    private String pathToGoblintConf;
    private String pathToCompilationDBDir;
    private String[] commands;

    private Logger log;

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
     * @return the CLI command including the arguments to be executed.
     */
    public String[] getCommand() {
        return this.commands;
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
                log.info("New analysis started");
                MagpieServer server = (MagpieServer) consumer;
                boolean successful = generateJson();
                Collection<AnalysisResult> analysisResults = new ArrayList<>();
                if (successful) analysisResults.addAll(readResultsFromJson());
                server.consume(analysisResults, source());
            }
        }
    }


    public ProcessResult runCommand(File dirPath) throws IOException, InvalidExitValueException, InterruptedException, TimeoutException {
        String[] command = this.getCommand();
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
        this.commands = new String[]{"goblint", "--conf", pathToGoblintConf, "--set", "result", "json-messages", "-o", pathToJsonResult, pathToCompilationDBDir};

        try {
            // run command
            log.info("Goblint run with command: " + String.join(" ", this.getCommand()));
            ProcessResult commandRunProcess = this.runCommand(new File(System.getProperty("user.dir")));
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
            magpieServer.forwardMessageToClient(new MessageParams(MessageType.Error, "Running Goblint failed. " + e.getMessage()));
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

        Collection<GoblintAnalysisResult> results = new ArrayList<>();

        try {
            log.debug("Reading analysis results from json");
            // Read json objects as an array
            JsonArray resultArray = JsonParser.parseReader(new FileReader(new File(pathToJsonResult))).getAsJsonArray();
            GsonBuilder builder = new GsonBuilder();
            // Add deserializer for tags
            builder.registerTypeAdapter(GoblintResult.tag.class, new TagInterfaceAdapter());
            Gson gson = builder.create();
            // For each JsonObject
            for (int i = 0; i < resultArray.size(); i++) {
                // Deserailize them into GoblintResult objects
                GoblintResult goblintResult = gson.fromJson(resultArray.get(i), GoblintResult.class);
                // Convert GoblintResult object to a list of GoblintAnalysisResults
                results.addAll(goblintResult.convert());
            }
            log.debug("Analysis results read from json");

        } catch (JsonIOException | JsonSyntaxException | FileNotFoundException | MalformedURLException e) {
            throw new RuntimeException(e);
        }

        return results;
    }

    private boolean readGobPieConfiguration() {
        try {
            log.debug("Reading GobPie configuration from json");
            Gson gson = new GsonBuilder().create();
            // Read json object
            JsonObject jsonObject = JsonParser.parseReader(new FileReader(new File(pathToGobPieConf))).getAsJsonObject();
            // Convert json object to GobPieConfiguration object
            GobPieConfiguration gobpieConfiguration = gson.fromJson(jsonObject, GobPieConfiguration.class);
            pathToGoblintConf = System.getProperty("user.dir") + "/" + gobpieConfiguration.getGoblintConfPath();
            pathToCompilationDBDir = System.getProperty("user.dir") + "/" + gobpieConfiguration.getCompilationDatabaseDirPath();
            if (gobpieConfiguration.getGoblintConfPath().equals("") || gobpieConfiguration.getCompilationDatabaseDirPath().equals("")) {
                log.debug("Configuration parameters missing from GobPie configuration file");
                magpieServer.forwardMessageToClient(new MessageParams(MessageType.Error, "Configuration parameters missing from GobPie configuration file."));
                return false;
            }
            log.debug("GobPie configuration read from json");
        } catch (JsonIOException | JsonSyntaxException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            magpieServer.forwardMessageToClient(new MessageParams(MessageType.Error, "Could not locate GobPie configuration file." + e.getMessage()));
            return false;
        }
        return true;
    }

}
