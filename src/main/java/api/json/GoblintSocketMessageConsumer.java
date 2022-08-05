package api.json;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.json.MessageConstants;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.messages.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


/**
 * A message consumer that serializes messages to JSON and sends them to an output stream.
 *
 * @since 0.0.3
 */

public class GoblintSocketMessageConsumer implements MessageConsumer, MessageConstants {

    private final String encoding;
    private final MessageJsonHandler jsonHandler;
    private final Object outputLock = new Object();
    private final OutputStream output;

    private final Logger log = LogManager.getLogger(GoblintSocketMessageConsumer.class);

    public GoblintSocketMessageConsumer(OutputStream output, MessageJsonHandler jsonHandler) {
        this(output, StandardCharsets.UTF_8.name(), jsonHandler);
    }

    public GoblintSocketMessageConsumer(OutputStream output, String encoding, MessageJsonHandler jsonHandler) {
        this.output = output;
        this.encoding = encoding;
        this.jsonHandler = jsonHandler;
    }

    @Override
    public void consume(Message message) {
        try {
            String content = jsonHandler.serialize(message) + "\n";
            log.info(content);
            byte[] contentBytes = content.getBytes(encoding);
            synchronized (outputLock) {
                output.write(contentBytes);
                output.flush();
            }
        } catch (IOException exception) {
            throw new JsonRpcException(exception);
        }
    }

}
