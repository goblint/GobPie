package abstractdebugging;

/**
 * Thrown when the requested step is invalid.
 * Usually this occurs because the target node is ambiguous for some thread.
 */
public class IllegalStepException extends Exception {

    public IllegalStepException(String message) {
        super(message);
    }

}
