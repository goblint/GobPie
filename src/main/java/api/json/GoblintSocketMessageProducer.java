package api.json;

import org.apache.logging.log4j.LogManager;
import org.eclipse.lsp4j.jsonrpc.*;
import org.eclipse.lsp4j.jsonrpc.json.MessageConstants;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer;
import org.eclipse.lsp4j.jsonrpc.messages.Message;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A message producer that reads from an input stream and parses messages from JSON.
 *
 * @since 0.0.3
 */

public class GoblintSocketMessageProducer implements MessageProducer, Closeable, MessageConstants {

    private static final Logger LOG = Logger.getLogger(StreamMessageProducer.class.getName());

    private final MessageJsonHandler jsonHandler;
    private final MessageIssueHandler issueHandler;
    private final BufferedReader inputReader;

    private MessageConsumer callback;
    private boolean keepRunning;

    private final org.apache.logging.log4j.Logger log = LogManager.getLogger(GoblintSocketMessageConsumer.class);

    public GoblintSocketMessageProducer(InputStream input, MessageJsonHandler jsonHandler) {
        this(input, jsonHandler, null);
    }

    public GoblintSocketMessageProducer(InputStream input, MessageJsonHandler jsonHandler, MessageIssueHandler issueHandler) {
        this.jsonHandler = jsonHandler;
        this.issueHandler = issueHandler;
        this.inputReader = new BufferedReader(new InputStreamReader(input));
    }

    @Override
    public void listen(MessageConsumer callback) {
        if (keepRunning) {
            throw new IllegalStateException("This StreamMessageProducer is already running.");
        }
        this.keepRunning = true;
        this.callback = callback;
        try {
            while (keepRunning) {
                boolean result = handleMessage();
                if (!result)
                    keepRunning = false;
            } // while (keepRunning)
        } catch (IOException exception) {
            if (JsonRpcException.indicatesStreamClosed(exception)) {
                // Only log the error if we had intended to keep running
                if (keepRunning)
                    fireStreamClosed(exception);
            } else
                throw new JsonRpcException(exception);
            this.keepRunning = false;
        } finally {
            this.callback = null;
            this.keepRunning = false;
        }
    }

    /**
     * Log an error.
     */
    protected void fireError(Throwable error) {
        String message = error.getMessage() != null ? error.getMessage() : "An error occurred while processing an incoming message.";
        LOG.log(Level.SEVERE, message, error);
    }

    /**
     * Report that the stream was closed through an exception.
     */
    protected void fireStreamClosed(Exception cause) {
        String message = cause.getMessage() != null ? cause.getMessage() : "The input stream was closed.";
        LOG.log(Level.INFO, message, cause);
    }


    /**
     * Read the JSON content part of a message, parse it, and notify the callback.
     *
     * @return {@code true} if we should continue reading from the input stream, {@code false} if we should stop
     */
    protected boolean handleMessage() throws IOException {
        if (callback == null)
            callback = message -> LOG.log(Level.INFO, "Received message: " + message);

        try {
            String content = inputReader.readLine();
            log.info("Response read from socket.");
            try {
                if (content != null) {
                    Message message = jsonHandler.parseMessage(content);
                    callback.consume(message);
                } else {
                    return false;
                }
            } catch (MessageIssueException exception) {
                // An issue was found while parsing or validating the message
                if (issueHandler != null)
                    issueHandler.handle(exception.getRpcMessage(), exception.getIssues());
                else
                    fireError(exception);
                return false;
            }
        } catch (Exception exception) {
            // UnsupportedEncodingException can be thrown by String constructor
            // JsonParseException can be thrown by jsonHandler
            // We also catch arbitrary exceptions that are thrown by message consumers in order to keep this thread alive
            fireError(exception);
            return false;
        }
        return true;
    }

    @Override
    public void close() {
        keepRunning = false;
    }
}

