package api.messages;

import abstractdebugging.EdgeInfo;
import abstractdebugging.NodeInfo;
import api.json.MappedTuple;
import com.google.gson.JsonElement;

import java.util.List;

public class GoblintARGLookupResult {

    private String node;
    private String cfg_node;
    private GoblintLocation location;

    private List<Edge> prev;
    private List<Edge> next;

    public static class Edge implements MappedTuple {
        private Properties properties;
        private String other_node_id;

        @Override
        public String[] getMappedFields() {
            return new String[]{"properties", "other_node_id"};
        }

        public static class Properties implements MappedTuple {
            private String type_tag;
            private JsonElement data; // TODO: Decode data fields if they turn out to be useful

            @Override
            public String[] getMappedFields() {
                return new String[]{"type_tag", "data"};
            }
        }
    }

    public NodeInfo toNodeInfo() {
        NodeInfo nodeInfo = new NodeInfo(node, cfg_node, location);
        mapEdges(prev, nodeInfo.incomingCFGEdges(), nodeInfo.incomingReturnEdges(), nodeInfo.incomingEntryEdges());
        mapEdges(next, nodeInfo.outgoingCFGEdges(), nodeInfo.outgoingReturnEdges(), nodeInfo.outgoingEntryEdges());
        return nodeInfo;
    }

    private static void mapEdges(List<Edge> edges, List<EdgeInfo> cfgEdges, List<EdgeInfo> returnEdges, List<EdgeInfo> entryEdges) {
        for (var edge : edges) {
            EdgeInfo edgeInfo = new EdgeInfo(edge.other_node_id, edge.properties.data);
            switch (edge.properties.type_tag) {
                case "CFGEdge" -> cfgEdges.add(edgeInfo);
                case "InlineReturn" -> returnEdges.add(edgeInfo);
                case "InlineEntry" -> entryEdges.add(edgeInfo);
                default -> throw new IllegalStateException("Unsupported edge type: " + edge.properties.type_tag);
            }
        }
    }

}
