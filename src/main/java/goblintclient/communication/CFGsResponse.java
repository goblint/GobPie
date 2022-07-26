package goblintclient.communication;

import goblintclient.messages.GoblintCFG;

/**
 * The Class CFGsResponse.
 * <p>
 * Corresponding object to the jsonrpc response JSON for cfgs request.
 *
 * @author Karoliine Holter
 * @since 0.0.3
 */

public class CFGsResponse extends Response {

    // method: "cfgs" response:
    // {"id":0,"jsonrpc":"2.0","result":{"cfg":"digraph cfg {\n\tnode [id=\"\\N\", ... }}

    private GoblintCFG result;

    public GoblintCFG getResult() {
        return result;
    }
}

