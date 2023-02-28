package api.messages;

import abstractdebugging.CFGEdgeInfo;
import abstractdebugging.EdgeInfo;
import abstractdebugging.FunctionCallEdgeInfo;
import abstractdebugging.NodeInfo;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GoblintARGLookupResult {

    private String node;
    private String cfg_node;
    private String context;
    private String path;
    private GoblintLocation location;

    private List<Edge> prev;
    private List<Edge> next;

    public static class Edge {
        private Properties edge;
        private String node;

        public static class Properties {
            private JsonObject cfg;
            private JsonObject inlined;

            @SerializedName("return")
            private FunctionCall ret;
            private FunctionCall entry;

            public static class FunctionCall {
                JsonElement function;
                List<JsonElement> args;
                JsonElement lval;
            }
        }
    }

    public NodeInfo toNodeInfo() {
        NodeInfo nodeInfo = new NodeInfo(node, cfg_node, location, context, path);
        mapEdges(prev, nodeInfo.incomingCFGEdges, nodeInfo.incomingReturnEdges, nodeInfo.incomingEntryEdges);
        mapEdges(next, nodeInfo.outgoingCFGEdges, nodeInfo.outgoingReturnEdges, nodeInfo.outgoingEntryEdges);
        return nodeInfo;
    }

    private static void mapEdges(List<Edge> edges, List<CFGEdgeInfo> cfgEdges, List<FunctionCallEdgeInfo> returnEdges, List<FunctionCallEdgeInfo> entryEdges) {
        for (var edge : edges) {
            String cfgNode = edge.node.split("\\(")[0]; // TODO: Goblint should eventually return this itself
            if (edge.edge.cfg != null || edge.edge.inlined != null) {
                var properties = edge.edge.cfg != null ? edge.edge.cfg : edge.edge.inlined;
                CFGEdgeInfo edgeInfo = new CFGEdgeInfo(edge.node, cfgNode, properties.get("string").getAsString());
                cfgEdges.add(edgeInfo);
            } else if (edge.edge.ret != null) {
                var properties = edge.edge.ret;
                FunctionCallEdgeInfo edgeInfo = new FunctionCallEdgeInfo(edge.node, cfgNode,
                        toPrettyString(properties.function), properties.args.stream().map(GoblintARGLookupResult::toPrettyString).toList());
                returnEdges.add(edgeInfo);
            } else if (edge.edge.entry != null) {
                var properties = edge.edge.entry;
                FunctionCallEdgeInfo edgeInfo = new FunctionCallEdgeInfo(edge.node, cfgNode,
                        toPrettyString(properties.function), properties.args.stream().map(GoblintARGLookupResult::toPrettyString).toList());
                entryEdges.add(edgeInfo);
            } else {
                throw new IllegalStateException("Unknown edge type: " + edge);
            }
        }
    }

    private static String toPrettyString(JsonElement value) {
        if (value.isJsonPrimitive()) {
            return value.getAsString();
        } else {
            return value.toString();
        }
    }

}
