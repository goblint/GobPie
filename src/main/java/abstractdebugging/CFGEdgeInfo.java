package abstractdebugging;

public class CFGEdgeInfo extends EdgeInfo {

    public final String statementDisplayString;

    public CFGEdgeInfo(String nodeId, String cfgNodeId, String contextId, String pathId,
                       String statementDisplayString) {
        super(nodeId, cfgNodeId, contextId, pathId);
        this.statementDisplayString = statementDisplayString;
    }

}
