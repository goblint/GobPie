import java.lang.reflect.Type;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class TagInterfaceAdapter implements JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject.has("Category")) return jsonDeserializationContext.deserialize(jsonObject, GoblintResult.tag.Category.class);
        if (jsonObject.has("CWE")) return jsonDeserializationContext.deserialize(jsonObject, GoblintResult.tag.CWE.class);
        return null;
    }

}
