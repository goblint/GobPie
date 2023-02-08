package api;

import api.json.GoblintMessageJsonHandler;
import api.json.GoblintSocketMessageConsumer;
import api.json.GoblintSocketMessageProducer;
import api.jsonrpc.AutoClosingMessageProcessor;
import api.jsonrpc.CloseableEndpoint;
import gobpie.GobPieException;
import gobpie.GobPieExceptionType;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.MessageProducer;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Class {@link GoblintServiceLauncher}.
 * <p>
 * Creates the {@link RemoteEndpoint}, connects to the Goblint server socket
 * and creates the {@link MessageConsumer} and {@link ConcurrentMessageProcessor}.
 *
 * @author Karoliine Holter
 * @since 0.0.3
 */

public class GoblintServiceLauncher {

    private static final int SOCKET_CONNECT_RETRY_DELAY = 20;
    private static final int SOCKET_CONNECT_TOTAL_DELAY = 2000;

    private final ExecutorService executorService = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "goblint-server-worker");
        thread.setDaemon(true);
        return thread;
    });

    private final Logger log = LogManager.getLogger(GoblintServiceLauncher.class);


    /**
     * Tries to connect to the given Goblint server socket and returns a GoblintService connected to that socket.
     *
     * @throws GobPieException if connecting failed because the socket did not exist or was not accepting connections
     */
    public GoblintService connect(String goblintSocket) {
        try {
            // TODO: close after? (Currently not really needed since the socket being closed on the Goblint server side is usually followed by a restart of GobPie)
            AFUNIXSocket socket = AFUNIXSocket.newInstance();
            tryConnectSocket(socket, goblintSocket);
            GoblintService service = attachService(socket.getOutputStream(), socket.getInputStream());
            log.info("Goblint client connected");
            return service;
        } catch (IOException | InterruptedException e) {
            // These should never happen under normal usage
            return ExceptionUtils.rethrow(e);
        }
    }

    private void tryConnectSocket(AFUNIXSocket socket, String goblintSocket) throws InterruptedException {
        // Try to connect to socket. Polling is preferred to a file watcher because:
        // * Avoiding various race conditions with a file watcher is tricky.
        // * The file being created doesn't necessarily mean the socket is accepting connections.
        // Overall the performance impact and added delay of polling is negligible
        // and polling provides a much more robust guarantee that if the socket is accessible a connection will be made.
        for (int totalDelay = 0; totalDelay <= SOCKET_CONNECT_TOTAL_DELAY; totalDelay += SOCKET_CONNECT_RETRY_DELAY) {
            try {
                socket.connect(AFUNIXSocketAddress.of(new File(goblintSocket)));
                return;
            } catch (IOException ignored) {
            }
            // Ignore error; assume that server simply hasn't started yet and retry after waiting
            log.debug("Failed to connect to Goblint socket. Waiting for " + SOCKET_CONNECT_RETRY_DELAY + " ms and retrying");
            Thread.sleep(SOCKET_CONNECT_RETRY_DELAY);
        }
        throw new GobPieException("Connecting to Goblint server socket failed after retrying for " + SOCKET_CONNECT_TOTAL_DELAY + " ms", GobPieExceptionType.GOBPIE_EXCEPTION);
    }

    /**
     * Attaches a JSON-RPC endpoint to the given input and output stream and returns the attached service.
     * Once the remote service is closed call {@link CloseableEndpoint#close()} on the returned endpoint to ensure that all pending requests are rejected and do not hang indefinitely.
     *
     * @param outputStream Output stream used to send requests to remote service
     * @param inputStream  Input stream used to read requests from remote service
     */
    private GoblintService attachService(OutputStream outputStream, InputStream inputStream) {
        MessageJsonHandler messageJsonHandler = new GoblintMessageJsonHandler(ServiceEndpoints.getSupportedMethods(GoblintService.class));

        MessageProducer messageProducer = new GoblintSocketMessageProducer(inputStream, messageJsonHandler);

        MessageConsumer messageConsumer = new GoblintSocketMessageConsumer(outputStream, messageJsonHandler);
        RemoteEndpoint remoteEndpoint = new RemoteEndpoint(messageConsumer, ServiceEndpoints.toEndpoint(List.of()));
        messageJsonHandler.setMethodProvider(remoteEndpoint);

        CloseableEndpoint closeableEndpoint = new CloseableEndpoint(remoteEndpoint);

        ConcurrentMessageProcessor msgProcessor = new AutoClosingMessageProcessor(messageProducer, remoteEndpoint, closeableEndpoint);
        msgProcessor.beginProcessing(executorService);

        return ServiceEndpoints.toServiceObject(closeableEndpoint, GoblintService.class);
    }

}
