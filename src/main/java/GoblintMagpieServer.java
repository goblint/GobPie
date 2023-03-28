import magpiebridge.core.*;
import magpiebridge.file.SourceFileManager;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.concurrent.CompletableFuture;

public class GoblintMagpieServer extends MagpieServer {

    /**
     * Future that is completed once the configuration of the server has completed i.e. all analyses have been added.
     */
    private final CompletableFuture<Void> configurationDoneFuture = new CompletableFuture<>();

    /**
     * Instantiates a new MagpieServer using default {@link MagpieTextDocumentService} and {@link
     * MagpieWorkspaceService} with given {@link ServerConfiguration}.
     *
     * @param config the config
     */
    public GoblintMagpieServer(ServerConfiguration config) {
        super(config);
    }

    /**
     * Marks this server instance as fully configured, which allows the initialize request to complete.
     * The server will receive no communication other than the initialize request from the client before this is called.
     */
    public void configurationDone() {
        configurationDoneFuture.complete(null);
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        return configurationDoneFuture.thenCompose(_r -> super.initialize(params));
    }

    @Override
    protected void doSingleAnalysis(String language, Either<ServerAnalysis, ToolAnalysis> analysis, boolean rerun) {
        SourceFileManager fileManager = getSourceFileManager(language);
        Analysis<AnalysisConsumer> a = analysis.isLeft() ? analysis.getLeft() : analysis.getRight();
        if (a != null) {
            a.analyze(fileManager.getSourceFileModules().values(), this, rerun);
        }
    }

}
