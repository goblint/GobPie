import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeoutException;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;

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
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

public class GoblintAnalysis implements ServerAnalysis {

    final private MagpieServer magpieServer;
    private URL sourcefileURL;
    private String pathToJsonResult = System.getProperty("user.dir") + "/" + "analysisResults.json";
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
     * @param files    the files that have been opened in the editor.
     * @param consumer the server which consumes the analysis results.
     * @param rerun    tells if the analysis should be reran.
     */
    @Override
    public void analyze(Collection<? extends Module> files, AnalysisConsumer consumer, boolean rerun) {
        if (rerun) {
            if (consumer instanceof MagpieServer) {
                log.info("New analysis started");
                MagpieServer server = (MagpieServer) consumer;
                Collection<AnalysisResult> results = runAnalysisOnSelectedFiles(files);
                server.consume(results, source());
            }
        }
    }

    /**
     * Runs the command on CLI to generate the analysis results for opened files,
     * reads in the output and converts it into a collection of AnalysisResults.
     *
     * @param files the files that have been opened in the editor.
     */
    private Collection<AnalysisResult> runAnalysisOnSelectedFiles(Collection<? extends Module> files) {

        Collection<AnalysisResult> analysisResults = new ArrayList<>();

        for (Module file : files) {
            if (file instanceof SourceFileModule) {
                boolean successful = generateJson(file);
                if (successful) analysisResults.addAll(readResultsFromJson());
            }
        }
        return analysisResults;
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
     * @return returns true if goblint finished the analyse and json was generated sucessfully, false otherwise
     */
    private boolean generateJson(Module file) {
        SourceFileModule sourcefile = (SourceFileModule) file;
        try {
            // find sourcefile URL
            this.sourcefileURL = new URL(magpieServer.getClientUri(sourcefile.getURL().toString()));
            // file to be analyzed
            String fileToAnalyze = sourcefileURL.getFile();
            // construct command to run
            this.commands = new String[]{"goblint", "--conf", "goblint.json", "--set", "result", "json-messages", "-o", pathToJsonResult, fileToAnalyze};
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
            // For each JsonObject
            for (int i = 0; i < resultArray.size(); i++) {
                // Deserailize them into GoblintResult objects
                GsonBuilder builder = new GsonBuilder();
                builder.registerTypeAdapter(GoblintResult.tag.class, new TagInterfaceAdapter());
                Gson gson = builder.create();
                GoblintResult goblintResult = gson.fromJson(resultArray.get(i), GoblintResult.class);
                // Add sourcefileURL to object for generationg the position
                goblintResult.sourcefileURL = this.sourcefileURL;
                // Convert GoblintResult object to a list of GoblintAnalysisResults
                results.addAll(goblintResult.convert());
            }
            log.debug("Analysis results read from json");

        } catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return results;
    }

}
