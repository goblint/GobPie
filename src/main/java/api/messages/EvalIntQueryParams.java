package api.messages;

/**
 * @since 0.0.4
 */

public class EvalIntQueryParams {

    private String node;
    private String exp;

    public EvalIntQueryParams(String node, String exp) {
        this.node = node;
        this.exp = exp;
    }

}
