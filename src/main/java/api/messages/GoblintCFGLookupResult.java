package api.messages;

import abstractdebugging.CFGNodeInfo;

/**
 * @since 0.0.4
 */

public class GoblintCFGLookupResult {

    private String node;
    private GoblintLocation location;

    public CFGNodeInfo toCFGNodeInfo() {
        return new CFGNodeInfo(node, location);
    }

}
