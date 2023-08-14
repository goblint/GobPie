package abstractdebugging;

import api.messages.GoblintLocation;

/**
 * @since 0.0.4
 */

public record CFGNodeInfo(
        String cfgNodeId,
        GoblintLocation location
) {
}
