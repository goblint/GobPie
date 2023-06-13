package abstractdebugging;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @since 0.0.4
 */

public record BreakpointInfo(
        CFGNodeInfo cfgNode,
        @Nullable ConditionalExpression condition,
        List<NodeInfo> targetNodes
) {
}
