package api.messages;

import analysis.GoblintCFGAnalysisResult;
import magpiebridge.core.AnalysisResult;
import org.apache.commons.io.FilenameUtils;

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
    private GoblintLocation location;

    public String getType() {
        return type;
    }

    public AnalysisResult convert() {
        return new GoblintCFGAnalysisResult(location.toPosition(), funName);
    }

}

