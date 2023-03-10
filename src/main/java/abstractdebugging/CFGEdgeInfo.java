package abstractdebugging;

public class CFGEdgeInfo extends EdgeInfo {

    private final String statementDisplayString;

    public CFGEdgeInfo(String nodeId, String cfgNodeId, String contextId, String pathId,
                       String statementDisplayString) {
        super(nodeId, cfgNodeId, contextId, pathId);
        this.statementDisplayString = statementDisplayString;
    }

    public final String statementDisplayString() {
        return statementDisplayString;
    }

}
