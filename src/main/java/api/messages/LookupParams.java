package api.messages;

public class LookupParams {

    private String node;
    private GoblintLocation location;

    public LookupParams() {
    }

    public LookupParams(String node) {
        this.node = node;
    }

    public LookupParams(GoblintLocation location) {
        this.location = location;
    }

}
