package api.messages;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.math.BigInteger;

/**
 * @since 0.0.4
 */
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

    public boolean isTop() {
        // TODO: Is this the only possible representation of top?
        return raw.isJsonPrimitive() && "top".equals(raw.getAsString());
    }

    public boolean isBot() {
        // TODO: Is this the only possible representation of bot?
        return raw.isJsonPrimitive() && "bot".equals(raw.getAsString());
    }

    public boolean mayBeBool(boolean bool) {
        return mustBeBool(bool) || (!isBot() && this.bool == null);
    }

    public boolean mustBeBool(boolean bool) {
        return this.bool != null && this.bool == bool;
    }

}
