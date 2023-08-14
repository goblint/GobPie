package abstractdebugging;

/**
 * Thrown when the requested step is invalid.
 * Usually this occurs because the target node is ambiguous for some thread.
 *
 * @since 0.0.4
 */
public class IllegalStepException extends Exception {

    public IllegalStepException(String message) {
        super(message);
    }

}
