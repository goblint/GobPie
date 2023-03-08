package abstractdebugging;

import java.util.List;

public class ThreadState {

    public final String name;
    /**
     * Stack frames starting from topmost frame.
     */
    public final List<StackFrameState> frames;

    public ThreadState(String name, List<StackFrameState> frames) {
        this.name = name;
        this.frames = frames;
    }

    public StackFrameState currentFrame() {
        return frames.get(0);
    }

}
