package api.messages;

import analysis.GoblintCFGAnalysisResult;
import magpiebridge.core.AnalysisResult;

import java.util.List;

/**
 * The Class GoblintFunctionsResult.
 * <p>
 * Corresponding object to the Goblint functions request response results in JSON.
 * Converts the results from JSON to AnalysisResult requested by MagpieBridge.
 *
 * @author Karoliine Holter
 * @since 0.0.3
 */

public record GoblintFunctionsResult(String type, String funName, GoblintLocation location) {

    public List<AnalysisResult> convert() {
        var cfgResult = new GoblintCFGAnalysisResult(location.toPosition(), "show cfg", funName);
        if (funName.equals("main")) {
            AnalysisResult argResult = new GoblintCFGAnalysisResult(location.toPosition(), "show arg", "<arg>");
            return List.of(argResult, cfgResult);
        } else {
            return List.of(cfgResult);
        }
    }

}

