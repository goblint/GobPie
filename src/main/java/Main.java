import java.io.File;

import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.ServerConfiguration;
import magpiebridge.core.ToolAnalysis;

import analysis.GoblintAnalysis;
import goblintserver.GoblintClient;
import goblintserver.GoblintServer;

import org.eclipse.lsp4j.jsonrpc.messages.Either;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String... args) {

        // launch the server only if there is a GobPie conf file present
        if (new File(System.getProperty("user.dir") + "/" + "gobpie.json").exists()) {

            MagpieServer magpieServer = createMagpieServer();

            if (magpieServer == null) {
                log.info("Unable to launch MagpieBridge.");
            } else {
                magpieServer.launchOnStdio();
                magpieServer.doAnalysis("c", true);
                log.info("MagpieBridge server launched.");
            }
        }
    }


    private static MagpieServer createMagpieServer() {

        // set up configuration for MagpieServer
        ServerConfiguration serverConfig = new ServerConfiguration();
        serverConfig.setDoAnalysisByFirstOpen(false);
        MagpieServer magpieServer = new MagpieServer(serverConfig);

        // define language
        String language = "c";

        // start GoblintServer
        GoblintServer goblintServer = new GoblintServer(magpieServer);
        boolean gobServerStarted = goblintServer.startGoblintServer();
        if (!gobServerStarted) return null;

        // connect GoblintClient
        GoblintClient goblintClient = new GoblintClient(magpieServer);
        boolean goblintClientConnected = goblintClient.connectGoblitClient();
        if (!goblintClientConnected) return null;

        // add analysis to the MagpieServer
        ServerAnalysis serverAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintClient);
        Either<ServerAnalysis, ToolAnalysis> analysis = Either.forLeft(serverAnalysis);
        magpieServer.addAnalysis(analysis, language);

        return magpieServer;
    }

}
