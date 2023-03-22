package api.messages;

import javax.annotation.Nullable;

public class GoblintVarinfo {

    private long vid;
    private String name;
    private String original_name;
    private String role;
    private String function;
    private String type;
    private GoblintLocation location;

    public long getVid() {
        return vid;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getOriginalName() {
        return original_name;
    }

    public String getRole() {
        return role;
    }

    @Nullable
    public String getFunction() {
        return function;
    }

    public String getType() {
        return type;
    }

    public GoblintLocation getLocation() {
        return location;
    }

}
