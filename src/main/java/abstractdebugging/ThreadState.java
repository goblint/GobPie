package abstractdebugging;

import java.util.List;

/**
 * Represents the state of a debugging thread.
 * The state consists of a name, which is assigned when the thread is created, and a list of call stack frames.
 * <p>
 * A note about terminology: The topmost frame is the frame that was added by the latest function call and contains the current location of the thread.
 * It is located at index 0 in the list of frames.
 */
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
     * Get list of stack frames with the latest/topmost frame first.
     * Note: Do not manipulate the returned list directly. Use {@link #pushFrame} and {@link #popFrame} instead.
     */
    public List<StackFrameState> getFrames() {
        return frames;
    }

    public StackFrameState getCurrentFrame() {
        return frames.get(0);
    }

    public StackFrameState getPreviousFrame() {
        return frames.get(1);
    }

    public boolean hasPreviousFrame() {
        return frames.size() > 1;
    }

    public void pushFrame(StackFrameState frame) {
        frames.add(0, frame);
    }

    public void popFrame() {
        frames.remove(0);
    }

}
