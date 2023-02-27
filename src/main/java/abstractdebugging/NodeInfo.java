package abstractdebugging;

import api.messages.GoblintLocation;

import java.util.ArrayList;
import java.util.List;

public record NodeInfo(
        String nodeId,
        String cfgNodeId,
        GoblintLocation location,

        List<EdgeInfo> incomingCFGEdges,
        List<EdgeInfo> incomingEntryEdges,
        List<EdgeInfo> incomingReturnEdges,

        List<EdgeInfo> outgoingCFGEdges,
        List<EdgeInfo> outgoingEntryEdges,
        List<EdgeInfo> outgoingReturnEdges
) {

    public NodeInfo(String nodeId, String cfgNodeId, GoblintLocation location) {
        this(nodeId, cfgNodeId, location,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

}
