package abstractdebugging;

import java.util.List;

public class FunctionCallEdgeInfo extends EdgeInfo {

    public FunctionCallEdgeInfo(String nodeId, String cfgNodeId, String contextId, String pathId,
                                String function, List<String> args, boolean newThread) {
        super(nodeId, cfgNodeId, contextId, pathId);
        this.function = function;
        this.args = args;
        this.newThread = newThread;
    }

    public final String function;
    public final List<String> args;
    public final boolean newThread;

}
