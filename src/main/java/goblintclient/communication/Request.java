package goblintclient.communication;

import java.util.UUID;

/**
 * The Class Request.
 * <p>
 * Corresponding object to the jsonrpc request JSON.
 *
 * @author Karoliine Holter
 * @since 0.0.2
 */

public class Request {
    // {"jsonrpc":"2.0","id":0,"method":"analyze","params":{}}

    private final String jsonrpc = "2.0";
    private final UUID id;
    private final String method;
    private params params;

    static class params {
    }

    public Request(String method) {
        this.method = method;
        if (method.equals("analyze")) {
            this.params = new params();
        }
        this.id = UUID.randomUUID();
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public UUID getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public params getParams() {
        return params;
    }

}
