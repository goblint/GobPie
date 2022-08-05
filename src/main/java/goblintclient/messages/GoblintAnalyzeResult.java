package goblintclient.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class GoblintAnalyzeResult.
 * <p>
 * Corresponding object to the Goblint analyze request response results in JSON.
 *
 * @author Karoliine Holter
 * @since 0.0.3
 */

public class GoblintAnalyzeResult {

    private final List<String> status = new ArrayList<>();

    public List<String> getStatus() {
        return status;
    }

}
