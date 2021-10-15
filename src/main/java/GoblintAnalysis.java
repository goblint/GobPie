import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;

import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.ToolAnalysis;
import magpiebridge.core.MagpieServer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;

public class GoblintAnalysis implements ToolAnalysis {

    final private MagpieServer magpieServer;
    private URL sourcefileURL;
    private String pathToJsonResult = System.getProperty("user.dir") + "/" + "analysisResults.json";
    private String[] commands;

    public GoblintAnalysis(MagpieServer server) {
        this.magpieServer = server;
        magpieServer.addCommand("showresult", new ShowResultCommand());
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
    @Override
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
                generateJson(file);
                analysisResults.addAll(readResultsFromJson());
            }
        }
        return analysisResults;
    }

    /**
     * Runs the command in the project root directory
     * to let goblint generate the json file with analysis results.
     * If there is a goblint conf file present, it uses it in the command,
     * otherwise the command uses --enable dbg.debug option.
     *
     * @param file    the file on which to run the analysis.
     * @param command the command to run on the file.
     */
    private void generateJson(Module file) {
        SourceFileModule sourcefile = (SourceFileModule) file;

        try {
            // find sourcefile URL
            this.sourcefileURL = new URL(magpieServer.getClientUri(sourcefile.getURL().toString()));
            // file to be analyzed
            String fileToAnalyze = sourcefileURL.getFile();
            // construct command to run
            this.commands = new String[] { "goblint", "--conf", "goblint.json", "--set", "result", "json-messages", "-o", pathToJsonResult, fileToAnalyze };
            } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            // run command
            Process commandRunProcess = this.runCommand(new File(System.getProperty("user.dir")));
            commandRunProcess.waitFor();
            if (commandRunProcess.exitValue() != 0) 
                magpieServer.forwardMessageToClient(
                    new MessageParams(MessageType.Error, 
                        "Goblint exited with an error." + 
                        // the error message from command line
                        new BufferedReader(new InputStreamReader(commandRunProcess.getErrorStream()))
                        .lines().collect(Collectors.joining("\n"))));
        } catch (IOException | InterruptedException e) {
            magpieServer.forwardMessageToClient(new MessageParams(MessageType.Error, "Running Goblint failed. " + e.getMessage()));
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

        } catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return results;
    }

    /**
     * This method actually doesn't do anything, as we are not reading
     * any input from the command line. Has to be here for Magpie though.
     *
     * @return the analysis results of the tool converted in format of
     *         AnalysisResult
     */
    @Override
    public Collection<AnalysisResult> convertToolOutput() {
        return new HashSet<>();
    }

}
