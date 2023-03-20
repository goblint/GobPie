package api;

import api.messages.*;
import com.google.gson.JsonObject;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The GoblintService interface.
 * <p>
 * Lists all possible requests that can be sent to Goblint.
 *
 * @author Karoliine Holter
 * @since 0.0.3
 */

public interface GoblintService {

    @JsonRequest
    CompletableFuture<JsonObject> ping();

    // request:  {"jsonrpc":"2.0","id":0,"method":"analyze","params":{}}
    // response: {"id":0,"jsonrpc":"2.0","result":{"status":["Success"]}}
    @JsonRequest
    CompletableFuture<GoblintAnalysisResult> analyze(AnalyzeParams params);

    // request:  {"jsonrpc":"2.0","id":0,"method":"messages"}
    // response: {"id":0,"jsonrpc":"2.0","result":[{"tags":[{"Category":["Race"]}], ... }]}
    @JsonRequest
    CompletableFuture<List<GoblintMessagesResult>> messages();

    // request:  {"jsonrpc":"2.0","id":0,"method":"functions"}
    // response: {"id":0,"jsonrpc":"2.0","result":[{"funName":"qsort","location":{"file":"/home/ ... }]}
    @JsonRequest
    CompletableFuture<List<GoblintFunctionsResult>> functions();

    // request:  {"jsonrpc":"2.0","id":0,"method":"cfg", "params":{"fname":"main"}}
    // response: {"id":0,"jsonrpc":"2.0","result":{"cfg":"digraph cfg {\n\tnode [id=\"\\N\", ... }}
    @JsonRequest("cfg")
    CompletableFuture<GoblintCFGResult> cfg_dot(Params params);

    @JsonRequest("cfg/lookup")
    CompletableFuture<GoblintCFGLookupResult> cfg_lookup(CFGLookupParams params);

    // request:  {"jsonrpc":"2.0","id":0,"method":"node_state","params":{"nid":"fun2783"}}
    @JsonRequest("node_state")
    CompletableFuture<List<JsonObject>> cfg_state(NodeParams params);

    @JsonRequest("arg/dot")
    CompletableFuture<GoblintARGResult> arg_dot();

    @JsonRequest("arg/lookup")
    CompletableFuture<List<GoblintARGLookupResult>> arg_lookup(LookupParams params);

    @JsonRequest("arg/state")
    CompletableFuture<JsonObject> arg_state(ARGNodeParams params);

    @JsonRequest("arg/eval-int")
    CompletableFuture<EvalIntResult> arg_eval_int(ARGExprQueryParams params);

    @JsonRequest("cil/varinfos")
    CompletableFuture<List<GoblintVarinfo>> cil_varinfos();

    @JsonRequest
    CompletableFuture<Void> reset_config();

    // request:  {"jsonrpc":"2.0","id":0,"method":"read_config","params":{"fname":"goblint.json"}}
    // response: {"id":0,"jsonrpc":"2.0","result":null}
    //           {"id":0,"jsonrpc":"2.0","error":{"code":-32603,"message":"Json_encoding: Unexpected object field .."}}
    @JsonRequest
    CompletableFuture<Void> read_config(Params params);

}
