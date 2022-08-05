package api.messages;

import analysis.GoblintCFGAnalysisResult;
import magpiebridge.core.AnalysisResult;

import java.io.File;
import java.net.MalformedURLException;

/**
 * The Class GoblintFunctionsResult.
 * <p>
 * Corresponding object to the Goblint functions request response results in JSON.
 * Converts the results from JSON to AnalysisResult requested by MagpieBridge.
 *
 * @author Karoliine Holter
 * @since 0.0.3
 */

public class GoblintFunctionsResult {

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

    public AnalysisResult convert() {
        try {
            return new GoblintCFGAnalysisResult(
                    new GoblintPosition(location.line, location.endLine, location.column, location.endColumn, new File(location.file).toURI().toURL()),
                    funName,
                    new File(location.file).getName()
            );
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}

