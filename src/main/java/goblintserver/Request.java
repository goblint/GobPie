package goblintserver;

import java.util.UUID;

/**
 * The Class Request.
 * 
 * Corresponding object to the jsonrpc request JSON.
 * 
 * @author      Karoliine Holter
 * @since       0.0.2
 */

public class Request {
    // // {"jsonrpc":"2.0","id":0,"method":"analyze","params":{}}

    private String jsonrpc = "2.0";
    private UUID id;
    private String method;
    private params params;

    static class params {
    }

    public Request(String method) {
        this.method = method;
        if (method == "analyze") {
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
