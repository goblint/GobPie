package abstractdebugging;

public class StackFrameState {

    public NodeInfo node;
    public final boolean ambiguousFrame;

    public StackFrameState(NodeInfo node, boolean ambiguousFrame) {
        this.node = node;
        this.ambiguousFrame = ambiguousFrame;
    }
}
