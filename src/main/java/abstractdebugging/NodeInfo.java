package abstractdebugging;

import api.messages.GoblintLocation;

import java.util.ArrayList;
import java.util.List;

public record NodeInfo(
        String nodeId,
        String cfgNodeId,
        String contextId,
        String pathId,
        GoblintLocation location,
        String function,

        List<CFGEdgeInfo> incomingCFGEdges,
        List<FunctionCallEdgeInfo> incomingEntryEdges,
        List<FunctionCallEdgeInfo> incomingReturnEdges,

        List<CFGEdgeInfo> outgoingCFGEdges,
        List<FunctionCallEdgeInfo> outgoingEntryEdges,
        List<FunctionCallEdgeInfo> outgoingReturnEdges
) {

    public NodeInfo(String nodeId, String cfgNodeId, String contextId, String pathId, GoblintLocation location, String function) {
        this(nodeId, cfgNodeId, contextId, pathId, location, function,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public NodeInfo withLocation(GoblintLocation location) {
        return new NodeInfo(nodeId(), cfgNodeId(), contextId(), pathId(), location, function(),
                incomingCFGEdges(), incomingEntryEdges(), incomingReturnEdges(),
                outgoingCFGEdges(), outgoingEntryEdges(), outgoingReturnEdges());
    }

}
