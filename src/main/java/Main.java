import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.ServerConfiguration;
import magpiebridge.core.ToolAnalysis;

import analysis.GoblintAnalysis;
import goblintserver.GoblintClient;
import goblintserver.GoblintServer;

import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static final String gobPieConfFileName = "gobpie.json";
    private static final Logger log = LogManager.getLogger(Main.class);
    private static MagpieServer magpieServer;

    public static void main(String... args) {

        int exit = createMagpieServer();
        launchMagpieServer();

        if (exit != 0) {
            magpieServer.forwardMessageToClient(
                new MessageParams(MessageType.Error, "GobPie extension is unable to analyse the code. Please check the output terminal of GobPie extension for more information."));
            switch (exit) {
                case 1:
                    log.error("Unable to fully start GobPie extension: Starting Goblint Server failed.");
                case 2:
                    log.error("Unable to fully start GobPie extension: Connecting Goblint Client to Goblint Server failed.");
                case 3: 
                    log.error("Unable to fully start GobPie extension: GobPie configuration file is missing.");
                    waitForGobPieConf();
            }

        }
    }


    /**
     * Method for creating MagpieBridge server. 
     * 
     * Only if the gobpie configuration file exists in the project root, 
     * GoblintServer, GoblintClient and the GoblintAnalysis class are created
     * for the extension to work as it should.
     * 
     * Otherwise just a server without an analysis added to it is created. 
     * This is due to the extension having to start something in able to not just crash.
     *
     * @return exit value:
     *          0 - if Goblint Server is created and successfully connected to MagPieBridge
     *          1 - if Goblint Server was not started successfully
     *          2 - if Goblint Client could not connect to Goblint Server
     *          3 - if GobPie configuration file was missing and running Goblint Server was not possible
     */

    private static int createMagpieServer() {
        
        // set up configuration for MagpieServer
        ServerConfiguration serverConfig = new ServerConfiguration();
        serverConfig.setDoAnalysisByFirstOpen(false);
        magpieServer = new MagpieServer(serverConfig);

        // try and create Goblint server only if there is a GobPie conf file present
        if (new File(System.getProperty("user.dir") + "/" + gobPieConfFileName).exists()) {
            // define language
            String language = "c";

            // start GoblintServer
            GoblintServer goblintServer = new GoblintServer(magpieServer, gobPieConfFileName);
            boolean gobServerStarted = goblintServer.startGoblintServer();
            if (!gobServerStarted) return 1;

            // connect GoblintClient
            GoblintClient goblintClient = new GoblintClient(magpieServer);
            boolean goblintClientConnected = goblintClient.connectGoblintClient();
            if (!goblintClientConnected) return 2;

            // add analysis to the MagpieServer
            ServerAnalysis serverAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintClient);
            Either<ServerAnalysis, ToolAnalysis> analysis = Either.forLeft(serverAnalysis);
            magpieServer.addAnalysis(analysis, language);

            return 0;
        } 
        
        return 3;
    }


    /**
     * Method for launching MagpieBridge server and doing the initial analysis.
     */

    private static void launchMagpieServer() {
        magpieServer.launchOnStdio();
        log.info("MagpieBridge server launched.");
        magpieServer.doAnalysis("c", true);      
    }


    /**
     * Method for waiting until GobPie configuration file is created
     * and then restarting the server so that analyses can be run.
     */

    private static void waitForGobPieConf() {

        // wait until GobPie configuration file is created before continuing
        WatchService watchService;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(System.getProperty("user.dir"));
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE );
            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (((Path) event.context()).equals(Paths.get(gobPieConfFileName))) {
                        magpieServer.exit();
                        break;
                    }
                }
                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            log.error("Restarting GobPie extension failed: " + e.getMessage());
        } 

   }

}
