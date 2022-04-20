package goblintclient.communication;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class MessagesResponse.
 * <p>
 * Corresponding object to the jsonrpc response JSON for analyze request.
 *
 * @author Karoliine Holter
 * @since 0.0.2
 */

public class AnalyzeResponse extends Response {
    // method: "analyze" response:
    // {"id":0,"jsonrpc":"2.0","result":{"status":["Success"]}}

    private result result;

    static class result {
        private final List<String> status = new ArrayList<>();

        public List<String> getStatus() {
            return status;
        }

    }

    public result getResult() {
        return result;
    }

}
