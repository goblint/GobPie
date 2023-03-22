package api.messages;

public class GlobalStateParams {

    private Long vid;
    private String node;

    private GlobalStateParams(Long vid, String node) {
        this.vid = vid;
        this.node = node;
    }

    public static GlobalStateParams all() {
        return new GlobalStateParams(null, null);
    }

    public static GlobalStateParams byVid(Long vid) {
        return new GlobalStateParams(vid, null);
    }

    public static GlobalStateParams byCFGNodeId(String cfgNodeId) {
        return new GlobalStateParams(null, cfgNodeId);
    }

}
