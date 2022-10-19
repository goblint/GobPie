package api;

import HTTPserver.GobPieHttpHandler;
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

    // Examples of requests used in this project:
    // {"jsonrpc":"2.0","id":0,"method":"read_config","params":{"fname":"goblint.json"}}
    // {"jsonrpc":"2.0","id":0,"method":"analyze","params":{}}
    // {"jsonrpc":"2.0","id":0,"method":"messages"}
    // {"jsonrpc":"2.0","id":0,"method":"functions"}
    // {"jsonrpc":"2.0","id":0,"method":"cfg", "params":{"fname":"main"}}
    // {"jsonrpc":"2.0","id":0,"method":"node_state","params":{"nid":"fun2783"}}

    // Examples of responses for the requests:
    // method: "analyze" response:
    // {"id":0,"jsonrpc":"2.0","result":{"status":["Success"]}}
    // method: "messages" response:
    // {"id":0,"jsonrpc":"2.0","result":[{"tags":[{"Category":["Race"]}], ... }]}
    // method: "functions" response:
    // {"id":0,"jsonrpc":"2.0","result":[{"funName":"qsort","location":{"file":"/home/ ... }]}
    // method: "cfg" response:
    // {"id":0,"jsonrpc":"2.0","result":{"cfg":"digraph cfg {\n\tnode [id=\"\\N\", ... }}

    @JsonRequest
    CompletableFuture<JsonObject> ping();

    @JsonRequest
    CompletableFuture<GoblintAnalysisResult> analyze(Params params);

    @JsonRequest
    CompletableFuture<List<GoblintMessagesResult>> messages();

    @JsonRequest
    CompletableFuture<List<GoblintFunctionsResult>> functions();

    @JsonRequest
    CompletableFuture<GoblintCFGResult> cfg(Params params);

    @JsonRequest
    CompletableFuture<List<JsonObject>> node_state(GobPieHttpHandler.NodeParams params);

    @JsonRequest
    CompletableFuture<Void> reset_config();

    @JsonRequest
    CompletableFuture<Void> read_config(Params params);

}
