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
    // Examples of requests used in this project:
    // {"jsonrpc":"2.0","id":0,"method":"analyze","params":{}}
    // {"jsonrpc":"2.0","id":0,"method":"messages"}
    // {"jsonrpc":"2.0","id":0,"method":"functions"}
    // {"jsonrpc":"2.0","id":0,"method":"cfg", "params":{"fname":"main"}}

    private final String jsonrpc = "2.0";
    private final UUID id;
    private final String method;
    private params params;

    static class params {
        private String fname;
    }

    public Request(String method) {
        this.method = method;
        if (method.equals("analyze")) {
            this.params = new params();
        }
        this.id = UUID.randomUUID();
    }

    public Request(String method, String fname) {
        this.method = method;
        this.params = new params();
        this.params.fname = fname;
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
