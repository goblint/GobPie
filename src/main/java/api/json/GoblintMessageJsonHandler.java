package api.json;

import api.messages.GoblintMessagesResult;
import com.google.gson.GsonBuilder;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethod;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.json.adapters.*;

import java.util.Map;

/**
 * An extension of the {@link MessageJsonHandler} class to disable the serializing of
 * missing params field to a null value and omitting the field instead by using {@link GoblintMessageTypeAdapter}.
 *
 * @since 0.0.3
 */

public class GoblintMessageJsonHandler extends MessageJsonHandler {

    public GoblintMessageJsonHandler(Map<String, JsonRpcMethod> supportedMethods) {
        super(supportedMethods);
    }

    @Override
    public GsonBuilder getDefaultGsonBuilder() {
        return new GsonBuilder()
                .registerTypeAdapterFactory(new CollectionTypeAdapter.Factory())
                .registerTypeAdapterFactory(new ThrowableTypeAdapter.Factory())
                .registerTypeAdapterFactory(new EitherTypeAdapter.Factory())
                .registerTypeAdapterFactory(new TupleTypeAdapters.TwoTypeAdapterFactory())
                .registerTypeAdapterFactory(new EnumTypeAdapter.Factory())
                .registerTypeAdapterFactory(new GoblintMessageTypeAdapter.Factory(this))
                .registerTypeAdapter(GoblintMessagesResult.Tag.class, new GoblintTagInterfaceAdapter());
    }

}
