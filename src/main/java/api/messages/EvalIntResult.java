package api.messages;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.math.BigInteger;

public class EvalIntResult {

    private JsonElement raw;

    @SerializedName("int")
    @Nullable
    private BigInteger int_;
    @Nullable
    private Boolean bool;

    public JsonElement getRaw() {
        return raw;
    }

    @Nullable
    public BigInteger getInt() {
        return int_;
    }

    @Nullable
    public Boolean getBool() {
        return bool;
    }

    public boolean isBot() {
        // TODO: Is this the only possible representation of bot?
        return raw.isJsonPrimitive() && "bot".equals(raw.getAsString());
    }

    public boolean mayBeBool(boolean bool) {
        // TODO: Is excluding bot enough to make this sound?
        return mustBeBool(bool) || (!isBot() && this.bool == null);
    }

    public boolean mustBeBool(boolean bool) {
        return this.bool != null && this.bool == bool;
    }

}
