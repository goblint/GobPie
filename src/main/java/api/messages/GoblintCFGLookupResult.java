package api.messages;

import abstractdebugging.CFGNodeInfo;

/**
 * @since 0.0.4
 */

public record GoblintCFGLookupResult(
        String node,
        GoblintLocation location) {

    public CFGNodeInfo toCFGNodeInfo() {
        return new CFGNodeInfo(node, location);
    }

}
