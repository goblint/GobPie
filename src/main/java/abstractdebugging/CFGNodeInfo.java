package abstractdebugging;

import api.messages.GoblintLocation;

public record CFGNodeInfo(
        String cfgNodeId,
        GoblintLocation location
) {
}
