package abstractdebugging;

public class CFGEdgeInfo extends EdgeInfo {

    public final String statementDisplayString;

    public CFGEdgeInfo(String nodeId, String cfgNodeId, String statementDisplayString) {
        super(nodeId, cfgNodeId);
        this.statementDisplayString = statementDisplayString;
    }

}
