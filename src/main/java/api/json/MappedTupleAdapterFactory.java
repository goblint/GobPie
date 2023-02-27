package api.json;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * TypeAdapterFactory that creates adapters that deserialize a MappedTuple object from a tuple into an object
 * based on the mapping returned by {@link MappedTuple#getMappedFields()}.
 * <p>
 * This is useful for deserializing tuples coming from Goblint into well-defined classes without resorting to custom tuple implementations in Java.
 */
public class MappedTupleAdapterFactory implements TypeAdapterFactory {

    // TODO: It would probably be cleaner to implement these using per-field annotations, but that requires more complicated reflection and plumbing code.

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!MappedTuple.class.isAssignableFrom(type.getRawType())) {
            return null;
        }

        TypeAdapter<T> baseTypeAdapter = gson.getDelegateAdapter(this, type);
        String[] fields;
        try {
            Constructor<?> constructor = type.getRawType().getConstructor();
            fields = ((MappedTuple) constructor.newInstance()).getMappedFields();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to create base instance for MappedTuple", e);
        }

        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }
                JsonObject object = baseTypeAdapter.toJsonTree(value).getAsJsonObject();
                JsonArray tuple = new JsonArray();
                for (String field : ((MappedTuple) value).getMappedFields()) {
                    tuple.add(object.get(field));
                }
                Streams.write(tuple, out);
            }

            @Override
            public T read(JsonReader in) {
                JsonElement value = Streams.parse(in);
                if (value.isJsonNull()) {
                    return null;
                }
                JsonArray tuple;
                if (value.isJsonArray()) {
                    tuple = value.getAsJsonArray();
                } else {
                    // Consider a non-array value equivalent to a tuple of arity 1 containing that value.
                    tuple = new JsonArray();
                    tuple.add(value);
                }
                JsonObject object = new JsonObject();
                int fieldCount = Math.min(tuple.size(), fields.length);
                for (int i = 0; i < fieldCount; i++) {
                    object.add(fields[i], tuple.get(i));
                }
                return baseTypeAdapter.fromJsonTree(object);
            }
        };
    }

}
