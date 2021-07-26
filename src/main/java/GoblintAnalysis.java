import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;

import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.ToolAnalysis;
import magpiebridge.core.MagpieServer;

public class GoblintAnalysis implements ToolAnalysis {

    final private MagpieServer magpieServer;
    private List<DbgResult> lines;
    private URL sourcefileURL;
    private String[] debugCommand = { "./goblint", "--enable", "dbg.debug" };
    private String[] commands;

    public GoblintAnalysis(MagpieServer server) {
        this.magpieServer = server;
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
     * Runs the command(s) on CLI, reads in the output and converts it into a
     * collection of AnalysisResults.
     *
     * @param files the files that have been opened in the editor.
     */
    private Collection<AnalysisResult> runAnalysisOnSelectedFiles(Collection<? extends Module> files) {

        Collection<AnalysisResult> results = new ArrayList<>();

        for (Module file : files) {
            if (file instanceof SourceFileModule) {
                SourceFileModule sourcefile = (SourceFileModule) file;
                try {
                    // find sourcefile URL
                    this.sourcefileURL = new URL(magpieServer.getClientUri(sourcefile.getURL().toString()));
                    String[] fileCommand = { sourcefileURL.toString().substring(5) };
                    this.commands = Stream.concat(Arrays.stream(debugCommand), Arrays.stream(fileCommand)).toArray(String[]::new);
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                try {
                    // run command
                    Process process = this.runCommand(new File(System.getProperty("user.dir") + "/analyzer"));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String[] clioutput = reader.lines().toArray(String[]::new);
                    reader.close();
                    // extract info from cli output
                    this.lines = convertLines(clioutput);
                    // convert info to AnalysisResult objects and add to results
                    results.addAll(convertToolOutput());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return results;
    }

    /**
     * Filters out the necessary information from each CLI output line.
     *
     * @param lines An array of CLI output lines.
     */
    private List<DbgResult> convertLines(String[] lines) {
        List<DbgResult> result = new ArrayList<>();
        for (String line : lines) {
            // extract message
            String message = match(".*(?= \\()", line);
            // extract line number
            int linenr = Integer.parseInt(match("(?<=c:)\\d*", line));
            // extract column number
            int columnStart = Integer.parseInt(match("(?<=\\d:)\\d*", line)) - 1;
            
            result.add(new DbgResult(message, linenr, columnStart, columnStart +6));
        }
        return result;
    }

    /**
     * A method that applies specified regex on given string and returns the result.
     *
     * @param regex The regex to be used on string.
     * @param input The string the regex is used on.
     * 
     * @return The match from regex or empty string otherwise.
     */
    private String match(String regex, String input) {
        Matcher matcher = Pattern.compile(regex).matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    /**
     * Converts the CLI output into a collection of Analysisresult objects
     *
     * @return the analysis results of the tool converted in format of
     *         AnalysisResult
     */
    @Override
    public Collection<AnalysisResult> convertToolOutput() {
        Set<AnalysisResult> results = new HashSet<>();

        for (DbgResult line : lines) {

            results.add(new DbgAnalysisResult(line, sourcefileURL));
        }
        return results;
    }

}
