package abstractdebugging;

import com.google.gson.JsonElement;

public record EdgeInfo(
        String otherNodeId,
        JsonElement data
) {

}
