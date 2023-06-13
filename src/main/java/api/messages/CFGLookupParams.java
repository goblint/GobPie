package api.messages;

/**
 * @since 0.0.4
 */

public class CFGLookupParams {

    private String node;
    private GoblintLocation location;

    private CFGLookupParams(String node, GoblintLocation location) {
        this.node = node;
        this.location = location;
    }

    public static CFGLookupParams byCFGNodeId(String cfgNodeId) {
        return new CFGLookupParams(cfgNodeId, null);
    }

    public static CFGLookupParams byLocation(GoblintLocation location) {
        return new CFGLookupParams(null, location);
    }

}
