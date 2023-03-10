package abstractdebugging;

import java.util.List;

public class ThreadState {

    private final String name;
    private final List<StackFrameState> frames;

    public ThreadState(String name, List<StackFrameState> frames) {
        this.name = name;
        this.frames = frames;
    }

    public String getName() {
        return name;
    }

    /**
     * Get list of stack frames with the latest frame first.
     */
    public List<StackFrameState> getFrames() {
        return frames;
    }

    public StackFrameState getCurrentFrame() {
        return frames.get(0);
    }

    public void pushFrame(StackFrameState frame) {
        frames.add(0, frame);
    }

    public void popFrame() {
        frames.remove(0);
    }

}
