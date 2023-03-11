package abstractdebugging;

import javax.annotation.Nullable;

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

    public boolean isAmbiguousFrame() {
        return ambiguousFrame;
    }

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
