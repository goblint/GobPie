import java.io.IOException;
import java.nio.file.*;

import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.ServerConfiguration;
import magpiebridge.core.ToolAnalysis;

import analysis.GoblintAnalysis;
import goblintserver.GobPieException;
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

        try {
            createMagpieServer();
            launchMagpieServer();
        } catch (GobPieException e) {
            launchMagpieServer();
            String message = "Unable to start GobPie extension: " + e.getMessage();
            magpieServer.forwardMessageToClient(
                new MessageParams(MessageType.Error, message + " Please check the output terminal of GobPie extension for more information.")
            );
            if (e.getCause() == null) log.error(message);
            else log.error(message + " Cause: " + e.getCause().getMessage());
            switch (e.getType()) {
                case GOBLINT_EXCEPTION:
                    break;
                case GOBPIE_EXCEPTION:
                    break;
                case GOBPIE_CONF_EXCEPTION:
                    waitForGobPieConf();
                    break;
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
     * @throws GobPieException
     */

    private static void createMagpieServer() throws GobPieException {
        
        // set up configuration for MagpieServer
        ServerConfiguration serverConfig = new ServerConfiguration();
        serverConfig.setDoAnalysisByFirstOpen(false);
        magpieServer = new MagpieServer(serverConfig);

        // define language
        String language = "c";

        // start GoblintServer
        GoblintServer goblintServer = new GoblintServer(gobPieConfFileName);
        goblintServer.startGoblintServer();

        // connect GoblintClient
        GoblintClient goblintClient = new GoblintClient();
        goblintClient.connectGoblintClient();

        // add analysis to the MagpieServer
        ServerAnalysis serverAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintClient);
        Either<ServerAnalysis, ToolAnalysis> analysis = Either.forLeft(serverAnalysis);
        magpieServer.addAnalysis(analysis, language);

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
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
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
            new MessageParams(MessageType.Error, "Restarting GobPie extension failed. Please check the output terminal of GobPie extension for more information.");
            log.error("Restarting GobPie extension failed: " + e.getMessage());
        } 

   }

}
