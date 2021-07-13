import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.lsp4j.DiagnosticSeverity;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;

import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.ToolAnalysis;
import magpiebridge.core.MagpieServer;

public class GoblintAnalysis implements ToolAnalysis {

    final private MagpieServer magpieServer;
    private Map<Integer, String> lines;
    private SourceFileModule sourcefile;
    final private String[] commands = { "./goblint", "../src/main/java/tutorial2/02-branch.c", "--enable",
            "dbg.debug" };

    String test;
    String test1;

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

    private Collection<AnalysisResult> runAnalysisOnSelectedFiles(Collection<? extends Module> files) {

        Collection<AnalysisResult> results = new ArrayList<>();

        for (Module file : files) {
            if (file instanceof SourceFileModule) {
                this.sourcefile = (SourceFileModule) file;
                try {
                    // this.lines = this.runCommandAndReturnOutput(new File(projectRootPath));
                    // this.lines = this.runCommandAndReturnOutput(new
                    // File(System.getProperty("user.dir") + "/analyzer"));
                    Process process = this.runCommand(new File(System.getProperty("user.dir") + "/analyzer"));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String[] clioutput = reader.lines().toArray(String[]::new);
                    reader.close();
                    this.lines = convertLines(clioutput);
                    results = convertToolOutput();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return results;
    }

    private Map<Integer, String> convertLines(String[] lines) {
        Map<Integer, String> result = new HashMap<>();
        for (String line : lines) {
            // extract message
            String message = match(".*(?= \\()", line);
            // extract line number
            int linenr = Integer.parseInt(match("(?<=c:)\\d*", line));
            result.put(linenr, message);
        }
        return result;
    }

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

        // find sourcefile URL
        URL sourcefileURL = null;
        try {
            sourcefileURL = new URL(magpieServer.getClientUri(sourcefile.getURL().toString()));
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        URL finalSourcefileURL = sourcefileURL;

        for (int linenr : lines.keySet()) {

            String message = lines.get(linenr);

            DiagnosticSeverity severity = DiagnosticSeverity.Hint;
            if (message.contains("unknown")) {
                //severity = DiagnosticSeverity.Error;
            }

            results.add(new DbgResult(linenr, finalSourcefileURL, severity, message));

            /*
            // create Position object for each analysis result (where to be placed in code
            // editor)
            final Position pos = new Position() {

                @Override
                public int getFirstCol() {
                    return 0;
                }

                @Override
                public int getFirstLine() {
                    return linenr;
                }

                @Override
                public int getFirstOffset() {
                    return 0;
                }

                @Override
                public int getLastCol() {
                    return 12;
                }

                @Override
                public int getLastLine() {
                    return linenr;
                }

                @Override
                public int getLastOffset() {
                    return 0;
                }

                @Override
                public int compareTo(SourcePosition arg0) {
                    return 0;
                }

                @Override
                public Reader getReader() {
                    return null;
                }

                @Override
                public URL getURL() {
                    return finalSourcefileURL;
                }
            };

            result.add(new AnalysisResult() {
                @Override
                public String code() {
                    String code;
                    try {
                        code = SourceCodeReader.getLinesInString(pos);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return code;
                }

                @Override
                public Kind kind() {
                    return Kind.Diagnostic;
                }

                @Override
                public Position position() {
                    return pos;
                }

                @Override
                public Iterable<Pair<Position, String>> related() {
                    return new ArrayList<>();
                }

                @Override
                public Pair<Position, String> repair() {
                    return Pair.make(pos, "Hardcoded text");
                }

                @Override
                public DiagnosticSeverity severity() {
                    return DiagnosticSeverity.Error;
                }

                @Override
                public String toString(boolean arg0) {
                    return lines.get(linenr);
                }

            });
            */
        }

        return results;
    }

}
