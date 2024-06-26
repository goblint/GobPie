package api.messages;

import java.util.List;

/**
 * The Class GoblintAnalysisResult.
 * <p>
 * Corresponding object to the Goblint analyze request response results in JSON.
 *
 * @author Karoliine Holter
 * @since 0.0.3
 */

public record GoblintAnalysisResult(List<String> status) {
}
