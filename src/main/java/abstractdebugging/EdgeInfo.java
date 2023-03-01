package abstractdebugging;

public class EdgeInfo {

    public final String nodeId;
    public final String cfgNodeId;
    public final String contextId;
    public final String pathId;

    // Location is returned by Goblint, but cannot be safely used here because we patch the location
    // based on outgoing edges which are not available here. (see AbstractDebuggingServer.lookupNodes for more info)

    public EdgeInfo(String nodeId, String cfgNodeId, String contextId, String pathId) {
        this.nodeId = nodeId;
        this.cfgNodeId = cfgNodeId;
        this.contextId = contextId;
        this.pathId = pathId;
    }

}
