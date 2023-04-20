package abstractdebugging;

import api.GoblintService;
import api.messages.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionException;

/**
 * Synchronous convenience methods for working with GoblintService for abstract debugging.
 * In the future this can be converted to an interface and mocked for testing purposes.
 */
public class ResultsService {

    private final GoblintService goblintService;

    public ResultsService(GoblintService goblintService) {
        this.goblintService = goblintService;
    }

    public List<NodeInfo> lookupNodes(LookupParams params) {
        return goblintService.arg_lookup(params)
                .thenApply(result -> result.stream()
                        .map(lookupResult -> {
                            NodeInfo nodeInfo = lookupResult.toNodeInfo();
                            if (!nodeInfo.outgoingReturnEdges().isEmpty() && nodeInfo.outgoingCFGEdges().isEmpty()) {
                                // Location of return nodes is generally the entire function.
                                // That looks strange, so we patch it to be only the end of the last line of the function.
                                // TODO: Maybe it would be better to adjust location when returning stack so the node info retains the original location
                                return nodeInfo.withLocation(new GoblintLocation(
                                        nodeInfo.location().getFile(),
                                        nodeInfo.location().getEndLine(), nodeInfo.location().getEndColumn(),
                                        nodeInfo.location().getEndLine(), nodeInfo.location().getEndColumn()
                                ));
                            } else {
                                return nodeInfo;
                            }
                        })
                        .toList())
                .join();
    }

    /**
     * @throws RequestFailedException if the node was not found or multiple nodes were found.
     */
    public NodeInfo lookupNode(String nodeId) {
        var nodes = lookupNodes(LookupParams.byNodeId(nodeId));
        return switch (nodes.size()) {
            case 0 -> throw new RequestFailedException("Node with id " + nodeId + " not found");
            case 1 -> nodes.get(0);
            default -> throw new RequestFailedException("Multiple nodes with id " + nodeId + " found");
        };
    }

    /**
     * Find a CFG node by its location. Any node that appears at this location or after it, is considered matching.
     *
     * @throws RequestFailedException if a matching node was not found.
     */
    public CFGNodeInfo lookupCFGNode(GoblintLocation location) {
        try {
            return goblintService.cfg_lookup(CFGLookupParams.byLocation(location)).join().toCFGNodeInfo();
        } catch (CompletionException e) {
            if (isRequestFailedError(e.getCause())) {
                throw new RequestFailedException(e.getCause().getMessage());
            }
            throw e;
        }
    }

    public JsonObject lookupState(String nodeId) {
        return goblintService.arg_state(new ARGStateParams(nodeId)).join();
    }

    public JsonElement lookupGlobalState() {
        return goblintService.global_state(GlobalStateParams.all()).join();
    }

    /**
     * @throws RequestFailedException if evaluating the expression failed, generally because the expression is syntactically or semantically invalid.
     */
    public EvalIntResult evaluateIntegerExpression(String nodeId, String expression) {
        try {
            return goblintService.arg_eval_int(new EvalIntQueryParams(nodeId, expression)).join();
        } catch (CompletionException e) {
            // Promote request failure to public API error because it is usually caused by the user entering an invalid expression
            // and the error message contains useful info about why the expression was invalid.
            if (isRequestFailedError(e.getCause())) {
                throw new RequestFailedException(e.getCause().getMessage());
            }
            throw e;
        }
    }

    /**
     * @throws RequestFailedException if evaluating the expression failed, generally because the expression is syntactically or semantically invalid.
     */
    public JsonElement evaluateExpression(String nodeId, String expression) {
        try {
            return goblintService.arg_eval(new EvalQueryParams(nodeId, expression)).join();
        } catch (CompletionException e) {
            // See note in evaluateIntegerExpression
            if (isRequestFailedError(e.getCause())) {
                throw new RequestFailedException(e.getCause().getMessage());
            }
            throw e;
        }
    }

    public List<GoblintVarinfo> getVarinfos() {
        return goblintService.cil_varinfos().join();
    }

    /**
     * Retrieves and returns a list of source files analyzed by Goblint.
     */
    public List<String> getGoblintTrackedFiles() {
        return goblintService.files().join()
                .values().stream()
                .flatMap(Collection::stream)
                .toList();
    }

    private static boolean isRequestFailedError(Throwable e) {
        return e instanceof ResponseErrorException re && re.getResponseError().getCode() == ResponseErrorCode.RequestFailed.getValue();
    }

}
