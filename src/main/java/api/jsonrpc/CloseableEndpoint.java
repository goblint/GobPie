package api.jsonrpc;

import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;

import java.io.IOException;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Endpoint that completes all requests exceptionally when closed.
 * This is useful because, for the built-in RemoteEndpoint, if the incoming pipe is broken then all already made requests will never complete.
 * <p>
 * Note that closing this endpoint is one-way i.e. all requests will be completed with an exception for the caller,
 * but will still reach the remote endpoint, if it is still capable of receiving requests.
 */
public class CloseableEndpoint implements Endpoint, AutoCloseable {

    private final WeakHashMap<CompletableFuture<?>, Void> pendingResponses = new WeakHashMap<>();
    private boolean closed = false;

    private final Endpoint inner;


    public CloseableEndpoint(Endpoint inner) {
        this.inner = inner;
    }


    /**
     * Closes the endpoint and completes all pending requests with a {@link JsonRpcException}.
     */
    @Override
    public void close() {
        synchronized (this) {
            closed = true;
            for (var response : pendingResponses.keySet()) {
                response.completeExceptionally(new JsonRpcException(new IOException("Endpoint closed")));
            }
        }
    }

    @Override
    public CompletableFuture<?> request(String method, Object parameter) {
        CompletableFuture<?> response = inner.request(method, parameter);
        synchronized (this) {
            if (closed) {
                response.completeExceptionally(new JsonRpcException(new IOException("Endpoint closed")));
            } else {
                pendingResponses.put(response, null);
            }
        }
        return response;
    }

    @Override
    public void notify(String method, Object parameter) {
        inner.notify(method, parameter);
    }

}