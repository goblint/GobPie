package goblintclient.communication;

import java.util.UUID;

/**
 * The Class Response.
 * <p>
 * Corresponding object to the jsonrpc response JSON.
 *
 * @author Karoliine Holter
 * @since 0.0.2
 */

public abstract class Response {
    // method: "analyze" response:
    // {"id":0,"jsonrpc":"2.0","result":{"status":["Success"]}}
    // method: "messages" response:
    // {"id":0,"jsonrpc":"2.0","result":[{"tags":[{"Category":["Race"]}], ... }]}

    private UUID id;
    private String jsonrpc = "2.0";

    public String getJsonrpc() {
        return jsonrpc;
    }

    public UUID getId() {
        return id;
    }

}
