
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.io.File;

import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.ServerConfiguration;
import magpiebridge.core.ToolAnalysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String... args) {
        // create server
        MagpieServer server = createServer();
        log.info("Server created");
        // launch the server only if there is a goblint conf file present
        if (new File(System.getProperty("user.dir") + "/" + "gobpie.json").exists()) {
            server.launchOnStdio();
            log.info("Server launched");
        }
    }

    private static MagpieServer createServer() {
        // set up configuration for MagpieServer
        ServerConfiguration serverConfig = new ServerConfiguration();
        MagpieServer server = new MagpieServer(serverConfig);
        // define language
        String language = "c";
        // add analysis to the MagpieServer
        ServerAnalysis serverAnalysis = new GoblintAnalysis(server);
        Either<ServerAnalysis, ToolAnalysis> analysis = Either.forLeft(serverAnalysis);
        server.addAnalysis(analysis, language);

        return server;
    }
}
