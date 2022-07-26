package goblintclient.communication;

import goblintclient.messages.GoblintCFG;

/**
 * The Class CFGResponse.
 * <p>
 * Corresponding object to the jsonrpc response JSON for cfg request.
 *
 * @author Karoliine Holter
 * @since 0.0.3
 */

public class CFGResponse extends Response {
    // method: "cfg" response:
    // {"id":0,"jsonrpc":"2.0","result":{"cfg":"digraph cfg {\n\tnode [id=\"\\N\", ... }}

    private GoblintCFG result;

    public GoblintCFG getResult() {
        return result;
    }
}

