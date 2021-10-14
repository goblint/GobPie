
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.io.File;

import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.ServerConfiguration;
import magpiebridge.core.ToolAnalysis;

public class Main {

    public static void main(String... args) {
        // create server
        MagpieServer server = createServer();
        // launch the server only if there is a goblint conf file present
        if (new File(System.getProperty("user.dir") + "/" + "goblint.json").exists()) {
            server.launchOnStdio();
        }
    }

    private static MagpieServer createServer() {
        // set up configuration for MagpieServer
        ServerConfiguration defaultConfig = new ServerConfiguration();
        // trigger analysis when file is opened
        defaultConfig.setDoAnalysisByOpen(true);
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
