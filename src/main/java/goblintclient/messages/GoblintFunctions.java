package goblintclient.messages;

import analysis.GoblintCFGAnalysisResult;
import magpiebridge.core.AnalysisResult;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Class GoblintFunctions.
 * <p>
 * Corresponding object to the Goblint functions request response results in JSON.
 * Converts the results from JSON to AnalysisResult requested by MagpieBridge.
 *
 * @author Karoliine Holter
 * @since 0.0.3
 */

public class GoblintFunctions {

    String type = getClass().getName();

    private String funName;
    private location location;

    static class location {
        private String file;
        private int line;
        private int column;
        private int endLine;
        private int endColumn;
    }

    public String getType() {
        return type;
    }

    public List<AnalysisResult> convert() throws MalformedURLException {
        List<AnalysisResult> results = new ArrayList<>();

        results.add(new GoblintCFGAnalysisResult(
                new GoblintPosition(location.line, location.endLine, location.column, location.endColumn, new File(location.file).toURI().toURL()),
                funName,
                new File(location.file).getName()
        ));

        return results;
    }

}

