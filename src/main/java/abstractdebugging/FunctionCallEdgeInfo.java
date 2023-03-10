package abstractdebugging;

import java.util.List;

public class FunctionCallEdgeInfo extends EdgeInfo {

    private final String function;
    private final List<String> args;
    private final boolean createsNewThread;

    public FunctionCallEdgeInfo(String nodeId, String cfgNodeId, String contextId, String pathId,
                                String function, List<String> args, boolean createsNewThread) {
        super(nodeId, cfgNodeId, contextId, pathId);
        this.function = function;
        this.args = args;
        this.createsNewThread = createsNewThread;
    }

    public final String function() {
        return function;
    }

    public final List<String> args() {
        return args;
    }

    public final boolean createsNewThread() {
        return createsNewThread;
    }

}
