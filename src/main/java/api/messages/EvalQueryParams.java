package api.messages;

public class EvalQueryParams {

    private String node;
    private String exp;
    private Long vid;

    public EvalQueryParams(String node, String exp) {
        this.node = node;
        this.exp = exp;
    }

    public EvalQueryParams(String node, Long vid) {
        this.node = node;
        this.vid = vid;
    }

}
