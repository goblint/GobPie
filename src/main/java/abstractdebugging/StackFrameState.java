package abstractdebugging;

public class StackFrameState {

    public NodeInfo node;
    public final boolean ambiguousFrame;
    public final int localThreadIndex;

    public StackFrameState(NodeInfo node, boolean ambiguousFrame, int localThreadIndex) {
        this.node = node;
        this.ambiguousFrame = ambiguousFrame;
        this.localThreadIndex = localThreadIndex;
    }
}
