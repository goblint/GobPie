import HTTPserver.GobPieHTTPServer;
import abstractdebugging.AbstractDebuggingServerLauncher;
import analysis.GoblintAnalysis;
import analysis.ShowCFGCommand;
import api.GoblintService;
import api.GoblintServiceLauncher;
import api.messages.Params;
import goblintserver.GoblintServer;
import gobpie.GobPieConfReader;
import gobpie.GobPieConfiguration;
import gobpie.GobPieException;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.ServerConfiguration;
import magpiebridge.core.ToolAnalysis;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.io.File;

public class Main {

    private static final String gobPieConfFileName = "gobpie.json";
    private static final String abstractDebuggingServerSocket = "gobpie_adb.sock";

    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String... args) {

        MagpieServer magpieServer = createMagpieServer();

        try {
            // Read GobPie configuration file
            GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, gobPieConfFileName);
            GobPieConfiguration gobpieConfiguration = gobPieConfReader.readGobPieConfiguration();

            // Start GoblintServer
            GoblintServer goblintServer = startGoblintServer(magpieServer);

            // Connect GoblintService and read configuration
            GoblintService goblintService = connectGoblintService(magpieServer, gobpieConfiguration, goblintServer);

            // Add analysis
            addAnalysis(magpieServer, gobpieConfiguration, goblintServer, goblintService);

            // Launch magpieServer
            magpieServer.launchOnStdio();
            log.info("MagpieBridge server launched.");

            // Launch abstract debugging server
            launchAbstractDebuggingServer(goblintService);
            log.info("Abstract debugging server launched");
        } catch (GobPieException e) {
            String message = e.getMessage();
            String terminalMessage;
            if (e.getCause() == null) terminalMessage = message;
            else terminalMessage = message + " Cause: " + e.getCause().getMessage();
            forwardErrorMessageToClient(magpieServer, message, terminalMessage);
            switch (e.getType()) {
                case GOBLINT_EXCEPTION:
                    break;
                case GOBPIE_EXCEPTION:
                    break;
                case GOBPIE_CONF_EXCEPTION:
                    break;
            }
        }

    }


    /**
     * Method for creating and launching MagpieBridge server.
     */

    private static MagpieServer createMagpieServer() {
        // set up configuration for MagpieServer
        ServerConfiguration serverConfig = new ServerConfiguration();
        serverConfig.setDoAnalysisByFirstOpen(true);
        serverConfig.setUseMagpieHTTPServer(false);
        return new MagpieServer(serverConfig);
    }


    /**
     * Starts Goblint server.
     *
     * @throws GobPieException if running the server start command fails
     */
    private static GoblintServer startGoblintServer(MagpieServer magpieServer) {
        GoblintServer goblintServer = new GoblintServer(magpieServer);
        if (log.isDebugEnabled()) {
            log.debug("Goblint version info:\n" + goblintServer.checkGoblintVersion());
        }
        goblintServer.startGoblintServer();

        return goblintServer;
    }


    /**
     * Connects the Goblint service to the Goblint server and reads the Goblint configuration file.
     *
     * @throws GobPieException if connecting fails
     */
    private static GoblintService connectGoblintService(MagpieServer magpieServer, GobPieConfiguration gobpieConfiguration, GoblintServer goblintServer) {
        GoblintServiceLauncher launcher = new GoblintServiceLauncher();
        GoblintService goblintService = launcher.connect(goblintServer.getGoblintSocket());

        // Read Goblint configurations
        goblintService.read_config(new Params(new File(gobpieConfiguration.getGoblintConf()).getAbsolutePath()))
                .exceptionally(ex -> {
                    String msg = "Goblint was unable to successfully read the configuration: " + ex.getMessage();
                    magpieServer.forwardMessageToClient(new MessageParams(MessageType.Warning, msg));
                    log.error(msg);
                    return null;
                })
                .join();

        return goblintService;
    }


    /**
     * Method for creating and adding Goblint analysis to MagpieBridge server.
     * <p>
     * Creates the GoblintAnalysis classes.
     */
    private static void addAnalysis(MagpieServer magpieServer, GobPieConfiguration gobpieConfiguration,
                                    GoblintServer goblintServer, GoblintService goblintService) {
        // define language
        String language = "c";

        // add analysis to the MagpieServer
        ServerAnalysis serverAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobpieConfiguration);
        Either<ServerAnalysis, ToolAnalysis> analysis = Either.forLeft(serverAnalysis);
        magpieServer.addAnalysis(analysis, language);

        // add HTTP server for showing CFGs, only if the option is specified in the configuration
        if (gobpieConfiguration.getShowCfg()) {
            String httpServerAddress = new GobPieHTTPServer(goblintService).start();
            magpieServer.addHttpServer(httpServerAddress);
            magpieServer.addCommand("showcfg", new ShowCFGCommand(httpServerAddress));
        }
    }


    /**
     * Launch abstract debugging server
     *
     * @throws GobPieException if creating domain socket for server fails
     */
    private static void launchAbstractDebuggingServer(GoblintService goblintService) {
        AbstractDebuggingServerLauncher launcher = new AbstractDebuggingServerLauncher(goblintService);
        launcher.launchOnDomainSocket(abstractDebuggingServerSocket);
    }


    /**
     * Method for forwarding Error messages to MagpieServer.
     *
     * @param popUpMessage    The message shown on the pop-up message.
     * @param terminalMessage The message shown in the terminal.
     */

    private static void forwardErrorMessageToClient(MagpieServer magpieServer, String popUpMessage, String terminalMessage) {
        magpieServer.forwardMessageToClient(
                new MessageParams(MessageType.Error, "Unable to start GobPie extension: " + popUpMessage + " Please check the output terminal of GobPie extension for more information.")
        );
        log.error(terminalMessage);
    }

}
