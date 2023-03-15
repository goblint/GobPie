package abstractdebugging;

/**
 * Exception thrown by {@link AbstractDebuggingServer} wrapper methods
 * when a syntactically valid request to the Goblint server fails for a known/expected reason.
 */
public class RequestFailedException extends RuntimeException {

    public RequestFailedException(String message) {
        super(message);
    }

}
