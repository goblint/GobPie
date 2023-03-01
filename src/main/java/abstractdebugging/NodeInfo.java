package abstractdebugging;

import api.messages.GoblintLocation;

import java.util.ArrayList;
import java.util.List;

public final class NodeInfo {
    public final String nodeId;
    public final String cfgNodeId;
    public final String contextId;
    public final String pathId;
    public final GoblintLocation location;

    public final List<CFGEdgeInfo> incomingCFGEdges;
    public final List<FunctionCallEdgeInfo> incomingEntryEdges;
    public final List<FunctionCallEdgeInfo> incomingReturnEdges;

    public final List<CFGEdgeInfo> outgoingCFGEdges;
    public final List<FunctionCallEdgeInfo> outgoingEntryEdges;
    public final List<FunctionCallEdgeInfo> outgoingReturnEdges;

    public NodeInfo(
            String nodeId, String cfgNodeId, String contextId, String pathId, GoblintLocation location,
            List<CFGEdgeInfo> incomingCFGEdges, List<FunctionCallEdgeInfo> incomingEntryEdges, List<FunctionCallEdgeInfo> incomingReturnEdges,
            List<CFGEdgeInfo> outgoingCFGEdges, List<FunctionCallEdgeInfo> outgoingEntryEdges, List<FunctionCallEdgeInfo> outgoingReturnEdges
    ) {
        this.nodeId = nodeId;
        this.cfgNodeId = cfgNodeId;
        this.contextId = contextId;
        this.pathId = pathId;
        this.location = location;
        this.incomingCFGEdges = incomingCFGEdges;
        this.incomingEntryEdges = incomingEntryEdges;
        this.incomingReturnEdges = incomingReturnEdges;
        this.outgoingCFGEdges = outgoingCFGEdges;
        this.outgoingEntryEdges = outgoingEntryEdges;
        this.outgoingReturnEdges = outgoingReturnEdges;
    }

    public NodeInfo(String nodeId, String cfgNodeId, String contextId, String pathId, GoblintLocation location) {
        this(nodeId, cfgNodeId, contextId, pathId, location,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public NodeInfo withLocation(GoblintLocation location) {
        return new NodeInfo(nodeId, cfgNodeId, contextId, pathId, location,
                incomingCFGEdges, incomingEntryEdges, incomingReturnEdges,
                outgoingCFGEdges, outgoingEntryEdges, outgoingReturnEdges);
    }

    @Override
    public String toString() {
        return "NodeInfo[" +
                "nodeId=" + nodeId + ", " +
                "cfgNodeId=" + cfgNodeId + ", " +
                "location=" + location + ", " +
                "incomingCFGEdges=" + incomingCFGEdges + ", " +
                "incomingEntryEdges=" + incomingEntryEdges + ", " +
                "incomingReturnEdges=" + incomingReturnEdges + ", " +
                "outgoingCFGEdges=" + outgoingCFGEdges + ", " +
                "outgoingEntryEdges=" + outgoingEntryEdges + ", " +
                "outgoingReturnEdges=" + outgoingReturnEdges + ']';
    }

}
