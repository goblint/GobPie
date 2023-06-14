package abstractdebugging;

import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.MessageIssueException;
import org.eclipse.lsp4j.jsonrpc.messages.Message;

/**
 * A wrapping lsp4j MessageConsumer that allows queueing events that will run after a message is sent.
 * This is a reasonably clean workaround for <a href="https://github.com/eclipse-lsp4j/lsp4j/issues/229">lsp4j issue #229</a>.
 * TODO: Try to upstream an implementation of event queueing to lsp4j?
 * TODO: If <a href="https://github.com/eclipse-lsp4j/lsp4j/issues/229">lsp4j issue #229</a> ever gets resolved then replace this with the implemented solution.
 *
 * @author Juhan Oskar Hennoste
 */
public class EventQueueMessageConsumer implements MessageConsumer {

    private final MessageConsumer inner;
    private final EventQueue eventQueue;

    public EventQueueMessageConsumer(MessageConsumer inner, EventQueue eventQueue) {
        if (eventQueue == null) {
            throw new IllegalArgumentException("eventQueue must not be null");
        }
        this.inner = inner;
        this.eventQueue = eventQueue;
    }

    @Override
    public void consume(Message message) throws MessageIssueException, JsonRpcException {
        inner.consume(message);
        eventQueue.runAll();
    }

}

