package goblintclient.communication;

import goblintclient.messages.GoblintFunctions;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class FunctionsResponse.
 * <p>
 * Corresponding object to the jsonrpc response JSON for functions request.
 *
 * @author Karoliine Holter
 * @since 0.0.3
 */

public class FunctionsResponse extends Response {
    // method: "functions" response:
    // {"id":0,"jsonrpc":"2.0","result":[{"funName":"qsort","location":{"file":"/home/ ... }]}

    private final List<GoblintFunctions> result = new ArrayList<>();

    public List<GoblintFunctions> getResult() {
        return result;
    }
}
