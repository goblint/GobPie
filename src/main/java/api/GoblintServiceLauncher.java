package api;

import api.json.GoblintMessageJsonHandler;
import api.json.GoblintSocketMessageConsumer;
import api.json.GoblintSocketMessageProducer;
import gobpie.GobPieException;
import gobpie.GobPieExceptionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.jsonrpc.json.ConcurrentMessageProcessor;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The Class {@link GoblintServiceLauncher}.
 * <p>
 * Creates the {@link RemoteEndpoint}, connects to the Goblint server socket
 * and creates the {@link MessageConsumer} and {@link ConcurrentMessageProcessor}.
 *
 * @author Karoliine Holter
 * @since 0.0.3
 */

public class GoblintServiceLauncher implements Launcher<GoblintService> {

    private final RemoteEndpoint remoteEndpoint;
    private final GoblintService proxy;
    private final ConcurrentMessageProcessor msgProcessor;
    private final ExecutorService execService;

    private final Logger log = LogManager.getLogger(GoblintServiceLauncher.class);


    private GoblintServiceLauncher(RemoteEndpoint remoteEndpoint,
                                   GoblintService proxy,
                                   ConcurrentMessageProcessor msgProcessor,
                                   ExecutorService execService) {
        this.remoteEndpoint = remoteEndpoint;
        this.proxy = proxy;
        this.msgProcessor = msgProcessor;
        this.execService = execService;
    }

    @Override
    public Future<Void> startListening() {
        return msgProcessor.beginProcessing(execService);
    }

    @Override
    public GoblintService getRemoteProxy() {
        return proxy;
    }

    @Override
    public RemoteEndpoint getRemoteEndpoint() {
        return remoteEndpoint;
    }


    /**
     * The launcher builder wires up all components for JSON-RPC communication.
     */

    public static class Builder extends Launcher.Builder<GoblintService> {

        private final String goblintSocketName = "goblint.sock";

        private OutputStream outputStream;
        private InputStream inputStream;

        private final Logger log = LogManager.getLogger(Builder.class);


        public GoblintServiceLauncher create(GoblintClient localEndpoint) {
            connectSocketStreams();

            setLocalService(localEndpoint);
            setRemoteInterface(GoblintService.class);

            // Create the JSON handler, remote endpoint and remote proxy
            MessageJsonHandler messageJsonHandler = new GoblintMessageJsonHandler(getSupportedMethods());
            RemoteEndpoint remoteEndpoint = createRemoteEndpoint(messageJsonHandler);
            messageJsonHandler.setMethodProvider(remoteEndpoint);
            GoblintService remoteProxy = createProxy(remoteEndpoint);
            localEndpoint.connect(remoteProxy);

            // Create the message processor
            GoblintSocketMessageProducer reader = new GoblintSocketMessageProducer(inputStream, messageJsonHandler);
            MessageConsumer messageConsumer = wrapMessageConsumer(remoteEndpoint);
            ConcurrentMessageProcessor msgProcessor = createMessageProcessor(reader, messageConsumer, remoteProxy);
            ExecutorService execService = executorService != null ? executorService : Executors.newCachedThreadPool();

            return new GoblintServiceLauncher(remoteEndpoint, remoteProxy, msgProcessor, execService);
        }

        /**
         * Create the remote endpoint that communicates with the local services.
         */

        @Override
        protected RemoteEndpoint createRemoteEndpoint(MessageJsonHandler messageJsonHandler) {
            GoblintSocketMessageConsumer messageConsumer = new GoblintSocketMessageConsumer(outputStream, messageJsonHandler);
            MessageConsumer outgoingMessageStream = wrapMessageConsumer(messageConsumer);
            Endpoint localEndpoint = ServiceEndpoints.toEndpoint(localServices);
            return new RemoteEndpoint(outgoingMessageStream, localEndpoint);
        }

        /**
         * Method for connecting to Goblint server socket.
         *
         * @throws GobPieException in case Goblint socket is missing or the client was unable to connect to the socket.
         */

        public void connectSocketStreams() {
            try {
                AFUNIXSocket socket = AFUNIXSocket.newInstance(); // TODO: close after
                socket.connect(AFUNIXSocketAddress.of(new File(goblintSocketName)));
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();
                log.info("Goblint client connected.");
            } catch (IOException e) {
                throw new GobPieException("Connecting Goblint Client to Goblint socket failed.", e, GobPieExceptionType.GOBPIE_EXCEPTION);
            }
        }

    }

}
