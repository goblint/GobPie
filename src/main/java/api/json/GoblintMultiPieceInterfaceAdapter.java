package api.json;

import api.messages.GoblintMessagesResult;
import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * The Class GoblintMultiPieceInterfaceAdapter.
 * <p>
 * Implements the JsonDeserializer to deserialize json to GoblintResult objects.
 * In particular to differentiate between the Group and Piece (Single) classes.
 *
 * @author Karoliine Holter
 * @since 0.0.4
 */


public class GoblintMultiPieceInterfaceAdapter implements JsonDeserializer<Object> {
    @Override
    public Object deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject.has("group_text"))
            return jsonDeserializationContext.deserialize(jsonObject, GoblintMessagesResult.Group.class);
        if (jsonObject.has("text"))
            return jsonDeserializationContext.deserialize(jsonObject, GoblintMessagesResult.Piece.class);
        return null;
    }
}
