package api.jsonrpc;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.MessageProducer;
import org.eclipse.lsp4j.jsonrpc.json.ConcurrentMessageProcessor;

/**
 * The Class AutoClosingMessageProcessor.
 * <p>
 * A ConcurrentMessageProcessor that automatically closes the provided Closeable once message processing has ended.
 *
 * @author Juhan Oskar Hennoste
 * @since 0.0.3
 */
public class AutoClosingMessageProcessor extends ConcurrentMessageProcessor {

    private final AutoCloseable closeable;

    public AutoClosingMessageProcessor(MessageProducer messageProducer, MessageConsumer messageConsumer, AutoCloseable closeable) {
        super(messageProducer, messageConsumer);
        this.closeable = closeable;
    }

    @Override
    protected void processingEnded() {
        super.processingEnded();
        try {
            closeable.close();
        } catch (Exception e) {
            ExceptionUtils.rethrow(e); // Unreachable for CloseableEndpoint
        }
    }

}