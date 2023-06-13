package api.messages.params;

import api.messages.GoblintLocation;

/**
 * @since 0.0.4
 */

public class LookupParams {

    private String node;
    private String cfg_node;
    private GoblintLocation location;

    private LookupParams(String node, String cfg_node, GoblintLocation location) {
        this.node = node;
        this.cfg_node = cfg_node;
        this.location = location;
    }

    public static LookupParams entryPoint() {
        return new LookupParams(null, null, null);
    }

    public static LookupParams byNodeId(String nodeId) {
        return new LookupParams(nodeId, null, null);
    }

    public static LookupParams byCFGNodeId(String cfgNodeId) {
        return new LookupParams(null, cfgNodeId, null);
    }

    public static LookupParams byLocation(GoblintLocation location) {
        return new LookupParams(null, null, location);
    }

}
