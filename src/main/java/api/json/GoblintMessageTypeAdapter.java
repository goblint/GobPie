package api.json;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.json.adapters.MessageTypeAdapter;
import org.eclipse.lsp4j.jsonrpc.messages.Message;

import java.io.IOException;

/**
 * An extension of the {@link MessageTypeAdapter} class to disable the serializing of
 * missing params field to a null value and omitting the field instead.
 *
 * @since 0.0.3
 */

public class GoblintMessageTypeAdapter extends MessageTypeAdapter {

    public GoblintMessageTypeAdapter(MessageJsonHandler handler, Gson gson) {
        super(handler, gson);
    }

    @Override
    protected void writeNullValue(JsonWriter out) throws IOException {
        out.nullValue();
    }

    public static class Factory implements TypeAdapterFactory {

        private final MessageJsonHandler handler;

        public Factory(MessageJsonHandler handler) {
            this.handler = handler;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            if (!Message.class.isAssignableFrom(typeToken.getRawType())) return null;
            return (TypeAdapter<T>) new GoblintMessageTypeAdapter(handler, gson);
        }

    }

}
