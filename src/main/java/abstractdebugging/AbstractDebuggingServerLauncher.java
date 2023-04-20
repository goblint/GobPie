package abstractdebugging;

import gobpie.GobPieException;
import gobpie.GobPieExceptionType;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.debug.DebugLauncher;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Launcher for abstract debugging server.
 * Responsible for exposing the DAP-based abstract debugging server over a UNIX domain socket.
 *
 * @author Juhan Oskar Hennoste
 */
public class AbstractDebuggingServerLauncher {

    private final ResultsService resultsService;

    private final ExecutorService executorService = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "adb-server-worker");
        thread.setDaemon(true);
        return thread;
    });

    private final Logger log = LogManager.getLogger(AbstractDebuggingServerLauncher.class);

    public AbstractDebuggingServerLauncher(ResultsService resultsService) {
        this.resultsService = resultsService;
    }

    /**
     * Launch abstract debugging server on domain socket.
     * For each new connection to the domain socket a new AbstractDebuggingServer instance will be created and initialized.
     *
     * @param socketAddress address for the domain socket to bind to. the socket file will be created and cleaned up automatically
     * @throws GobPieException if creating domain socket fails
     */
    public void launchOnDomainSocket(String socketAddress) {
        // TODO: Maybe lsp4j has built-in support for listening on domain socket. If so then that should be used instead.
        AFUNIXServerSocket serverSocket;
        try {
            serverSocket = AFUNIXServerSocket.bindOn(AFUNIXSocketAddress.of(new File(socketAddress)), true);
        } catch (Throwable e) {
            throw new GobPieException("Failed to create domain socket for abstract debugging server on " + socketAddress, e, GobPieExceptionType.GOBPIE_EXCEPTION);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Close server socket when JVM shuts down. This will delete the socket file if possible.
            try {
                serverSocket.close();
            } catch (IOException e) {
                ExceptionUtils.rethrow(e);
            }
        }));
        executorService.submit(() -> listenOnDomainSocket(serverSocket));
    }

    private void listenOnDomainSocket(AFUNIXServerSocket serverSocket) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                AFUNIXSocket clientSocket = serverSocket.accept();
                log.info("Accepted new connection to abstract debugging server.");

                AbstractDebuggingServer abstractDebuggingServer = new AbstractDebuggingServer(resultsService);
                Launcher<IDebugProtocolClient> launcher = new DebugLauncher.Builder<IDebugProtocolClient>()
                        .setLocalService(abstractDebuggingServer)
                        .setRemoteInterface(IDebugProtocolClient.class)
                        .setInput(clientSocket.getInputStream())
                        .setOutput(clientSocket.getOutputStream())
                        .setExecutorService(executorService)
                        .traceMessages(log.isDebugEnabled() ? new PrintWriter(System.err) : null)
                        .create();
                abstractDebuggingServer.connectClient(launcher.getRemoteProxy());
                launcher.startListening();
            } catch (Throwable e) {
                log.error("Error accepting connection to abstract debugging server:", e);
            }
        }
    }

}
