package abstractdebugging;

import javax.annotation.Nullable;
import java.util.List;

public record BreakpointInfo(
        CFGNodeInfo cfgNode,
        @Nullable ConditionalExpression condition,
        List<NodeInfo> targetNodes
) {
}
