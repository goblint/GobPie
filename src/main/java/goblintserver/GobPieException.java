package goblintserver;

/**
 * The Class GobPieException.
 * 
 * @author      Karoliine Holter
 * @since       0.0.2
 */

public class GobPieException extends RuntimeException {

    private final GobPieExceptionType type;

    public GobPieException(String message, Throwable cause, GobPieExceptionType type) {
        super(message, cause);
		this.type = type;
	}

    public GobPieException(String message, GobPieExceptionType type) {
        super(message);
		this.type = type;
	}

    public GobPieExceptionType getType() {
        return type;
    }
    
}
