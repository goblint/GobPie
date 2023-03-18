package api.messages;

import abstractdebugging.CFGNodeInfo;

public class GoblintCFGLookupResult {

    private String node;
    private GoblintLocation location;

    public CFGNodeInfo toCFGNodeInfo() {
        return new CFGNodeInfo(node, location);
    }

}
