package api.json;

import api.messages.GoblintMessagesResult;
import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * The Class TagInterfaceAdapter.
 * <p>
 * Implements the JsonDeserializer to deserialize json to GoblintResult objects.
 * In particular to differentiate between the Category and CWE classes.
 *
 * @author Karoliine Holter
 * @since 0.0.1
 */

public class GoblintTagInterfaceAdapter implements JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject.has("Category"))
            return jsonDeserializationContext.deserialize(jsonObject, GoblintMessagesResult.tag.Category.class);
        if (jsonObject.has("CWE"))
            return jsonDeserializationContext.deserialize(jsonObject, GoblintMessagesResult.tag.CWE.class);
        return null;
    }

}
