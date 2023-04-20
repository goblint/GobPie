package abstractdebugging;

public class EdgeInfo {

    private final String nodeId;
    private final String cfgNodeId;
    private final String contextId;
    private final String pathId;

    // Target node location is returned by Goblint, but cannot be safely used here because we patch the location
    // based on outgoing edges which are not available here. (see AbstractDebuggingServer.lookupNodes for more info)

    public EdgeInfo(String nodeId, String cfgNodeId, String contextId, String pathId) {
        this.nodeId = nodeId;
        this.cfgNodeId = cfgNodeId;
        this.contextId = contextId;
        this.pathId = pathId;
    }

    public final String nodeId() {
        return nodeId;
    }

    public final String cfgNodeId() {
        return cfgNodeId;
    }

    public final String contextId() {
        return contextId;
    }

    public final String pathId() {
        return pathId;
    }

}
