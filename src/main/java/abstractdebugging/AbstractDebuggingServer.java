package abstractdebugging;

import api.GoblintService;
import magpiebridge.core.MagpieServer;
import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AbstractDebuggingServer implements IDebugProtocolServer {

    private final MagpieServer magpieServer;
    private final GoblintService goblintService;

    public AbstractDebuggingServer(MagpieServer magpieServer, GoblintService goblintService) {
        this.magpieServer = magpieServer;
        this.goblintService = goblintService;
    }

    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        return CompletableFuture.completedFuture(new Capabilities());
    }

    @Override
    public CompletableFuture<Void> launch(Map<String, Object> args) {
        return IDebugProtocolServer.super.launch(args);
    }

    @Override
    public CompletableFuture<Void> attach(Map<String, Object> args) {
        // Attach doesn't make sense for abstract debugging, but there is no way to signal to the client that attach is unsupported, so we just treat it as a launch request.
        return launch(args);
    }
}
