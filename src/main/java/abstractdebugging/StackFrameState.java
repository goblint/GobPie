package abstractdebugging;

import javax.annotation.Nullable;

/**
 * @since 0.0.4
 */

public class StackFrameState {

    private NodeInfo node;
    private NodeInfo lastReachableNode;
    private final boolean ambiguousFrame;
    private final int localThreadIndex;

    public StackFrameState(NodeInfo node, boolean ambiguousFrame, int localThreadIndex) {
        this.node = node;
        this.lastReachableNode = node;
        this.ambiguousFrame = ambiguousFrame;
        this.localThreadIndex = localThreadIndex;
    }

    /**
     * Current ARG node in this frame.
     */
    @Nullable
    public NodeInfo getNode() {
        return node;
    }

    /**
     * Returns the last reachable (non-null) node that this frame has been on.
     */
    @Nullable
    public NodeInfo getLastReachableNode() {
        return lastReachableNode;
    }

    /**
     * If frame is ambiguous. An ambiguous frame is a frame where multiple possible frames can exist in the same position in the call stack.
     */
    public boolean isAmbiguousFrame() {
        return ambiguousFrame;
    }

    /**
     * Local thread index. Two stack frames in the same stack with different local thread indexes belong to different program threads.
     */
    public int getLocalThreadIndex() {
        return localThreadIndex;
    }

    /**
     * Sets current node. Tracks last reachable (non-null) node.
     */
    public void setNode(NodeInfo node) {
        if (node != null) {
            this.lastReachableNode = node;
        }
        this.node = node;
    }

}
