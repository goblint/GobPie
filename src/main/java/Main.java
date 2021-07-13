
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.ServerConfiguration;
import magpiebridge.core.ToolAnalysis;

public class Main {

    public static void main(String... args) {
        // launch the server
        createServer().launchOnStdio();
    }

    private static MagpieServer createServer() {
        // set up configuration for MagpieServer
        ServerConfiguration defaultConfig = new ServerConfiguration();
        MagpieServer server = new MagpieServer(defaultConfig);
        // define language
        String language = "c";
        // add analysis to the MagpieServer
        ToolAnalysis toolAnalysis = new GoblintAnalysis(server);
        Either<ServerAnalysis, ToolAnalysis> analysis = Either.forRight(toolAnalysis);
        server.addAnalysis(analysis, language);

        return server;
    }
}
