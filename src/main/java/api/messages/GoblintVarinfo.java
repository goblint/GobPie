package api.messages;

import javax.annotation.Nullable;

/**
 * @since 0.0.4
 */

public record GoblintVarinfo(
        long vid,
        String name,
        @Nullable String original_name,
        String role,
        @Nullable String function,
        String type,
        GoblintLocation location) {
}
