package abstractdebugging;

public class EdgeInfo {

    public final String nodeId;
    public final String cfgNodeId;

    public EdgeInfo(String nodeId, String cfgNodeId) {
        this.nodeId = nodeId;
        this.cfgNodeId = cfgNodeId;
    }

}
