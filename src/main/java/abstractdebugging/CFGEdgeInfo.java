package abstractdebugging;

/**
 * @since 0.0.4
 */

public class CFGEdgeInfo extends EdgeInfo {

    private final String statementDisplayString;
    private final String lval;

    public CFGEdgeInfo(String nodeId, String cfgNodeId, String contextId, String pathId,
                       String statementDisplayString) {
        super(nodeId, cfgNodeId, contextId, pathId);
        this.lval = null;
        this.statementDisplayString = statementDisplayString;
    }

    public CFGEdgeInfo(String nodeId, String cfgNodeId, String contextId, String pathId,
                       String statementDisplayString, String lval) {
        super(nodeId, cfgNodeId, contextId, pathId);
        this.lval = lval;
        this.statementDisplayString = statementDisplayString;
    }

    public final String statementDisplayString() {
        return statementDisplayString;
    }

    public final String lval() {
        return lval;
    }

}
