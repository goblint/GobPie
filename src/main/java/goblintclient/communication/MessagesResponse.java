package goblintclient.communication;

import java.util.ArrayList;
import java.util.List;

import goblintclient.messages.GoblintMessages;

/**
 * The Class MessagesResponse.
 * <p>
 * Corresponding object to the jsonrpc response JSON for messages request.
 *
 * @author Karoliine Holter
 * @since 0.0.2
 */

public class MessagesResponse extends Response {
    // method: "messages" response:
    // {"id":0,"jsonrpc":"2.0","result":[{"tags":[{"Category":["Race"]}], ... }]}

    private final List<GoblintMessages> result = new ArrayList<>();

    public List<GoblintMessages> getResult() {
        return result;
    }
}
