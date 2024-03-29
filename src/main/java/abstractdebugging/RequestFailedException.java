package abstractdebugging;

import api.GoblintService;

/**
 * Exception thrown by {@link AbstractDebuggingServer} {@link GoblintService} wrapper methods
 * when a syntactically valid request to the Goblint server fails for a known/expected reason.
 *
 * @since 0.0.4
 */
public class RequestFailedException extends RuntimeException {

    public RequestFailedException(String message) {
        super(message);
    }

}
