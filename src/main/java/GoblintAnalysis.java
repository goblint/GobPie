import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
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
    private URL sourcefileURL;
    private String[] debugCommand = { "./goblint", "--enable", "dbg.debug" };
    private String[] deadcodeCommand = { "./goblint", "--enable", "dbg.print_dead_code" };
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
                // -------------------------------------------------------------------
                // Assertions:
                // run command on cli and get the output
                String[] debugclioutput = getCLIoutput(file, debugCommand);
                // extract info from cli output
                List<DbgResult> debugresults = convertDebugLines(debugclioutput);
                // convert DbgResult to DbgAnalysisresults and add to (final) results
                results.addAll(convertResults(debugresults));
                // -------------------------------------------------------------------
                // Dead code:
                String[] deadcodeclioutput = getCLIoutput(file, deadcodeCommand);
                List<DbgResult> deadcoderesults = convertDeadCodeLines(deadcodeclioutput);
                results.addAll(convertResults(deadcoderesults));
            }
        }
        return results;
    }

    /**
     * Runs the command(s) on CLI, reads in the lines as an array of strings and
     * returns the array.
     *
     * @param file    the file on which to run the analysis.
     * @param command the command to run on the file.
     * @return an array of cli output lines
     */
    private String[] getCLIoutput(Module file, String[] command) {
        SourceFileModule sourcefile = (SourceFileModule) file;
        String[] clioutput = {};

        try {
            // find sourcefile URL
            this.sourcefileURL = new URL(magpieServer.getClientUri(sourcefile.getURL().toString()));
            String[] fileCommand = { sourcefileURL.toString().substring(5) };
            this.commands = Stream.concat(Arrays.stream(command), Arrays.stream(fileCommand)).toArray(String[]::new);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            // run command
            Process process = this.runCommand(new File(System.getProperty("user.dir") + "/analyzer"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            clioutput = reader.lines().toArray(String[]::new);
            reader.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return clioutput;
    }

    private Collection<AnalysisResult> convertResults(List<DbgResult> dbgResultLines) {
        Set<AnalysisResult> results = new HashSet<>();

        // convert DbgResult to DbgAnalysisResult objects and return them
        for (DbgResult line : dbgResultLines) {
            results.add(new DbgAnalysisResult(line, sourcefileURL));
        }

        return results;
    }

    /**
     * Filters out the necessary information from each CLI output line for the
     * --enable --dbg.debug option.
     *
     * @param lines An array of CLI output lines.
     */
    private List<DbgResult> convertDebugLines(String[] lines) {
        List<DbgResult> result = new ArrayList<>();

        for (String line : lines) {
            // extract message
            String message = match(".*(?= \\()", line);
            // extract line number
            int linenr = Integer.parseInt(match("(?<=c:)\\d*", line));
            // extract column number
            int columnStart = Integer.parseInt(match("(?<=\\d:)\\d*", line)) - 1;

            result.add(new DbgResult(message, linenr, columnStart, columnStart + 6));
        }

        return result;
    }

    /**
     * Filters out the necessary information from each CLI output line for the
     * --enable --dbg.print_dead_code option.
     *
     * @param lines An array of CLI output lines.
     */
    private List<DbgResult> convertDeadCodeLines(String[] lines) {
        List<DbgResult> result = new ArrayList<>();
        
        for (String line : lines) {
            // extract line numbers
            String linenumbers = match("(?<=dead code on lines: )[\\d., ]*", line);

            if (!linenumbers.equals("")) {
                String[] splitnumbers = linenumbers.split(", ");
                for (String numbers : splitnumbers) {
                    if (numbers.contains("..")) {
                        String[] nrs = numbers.split("\\.\\.");
                        result.add(new DbgResult("Dead code on lines: " + nrs[0] + "-" + nrs[1], Integer.parseInt(nrs[0]), Integer.parseInt(nrs[1]), sourcefileURL));
                    } else {
                        result.add(new DbgResult("Dead code on line: " + numbers, Integer.parseInt(numbers), sourcefileURL));
                    }
                }
            }
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
        return new HashSet<>();
    }

}
