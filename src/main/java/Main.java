import HTTPserver.GobPieHTTPServer;
import abstractdebugging.AbstractDebuggingServerLauncher;
import abstractdebugging.ResultsService;
import analysis.GoblintAnalysis;
import magpiebridge.ShowCFGCommand;
import api.GoblintService;
import api.GoblintServiceLauncher;
import api.messages.params.Params;
import goblintserver.GoblintConfWatcher;
import goblintserver.GoblintServer;
import gobpie.GobPieConfReader;
import gobpie.GobPieConfiguration;
import gobpie.GobPieException;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.GoblintLanguageExtensionHandler;
import magpiebridge.GoblintMagpieServer;
import magpiebridge.GoblintServerConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import util.FileWatcher;

import java.io.File;
import java.nio.file.Path;

public class Main {

    private static final String gobPieConfFileName = "gobpie.json";

    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String... args) {

        GoblintMagpieServer magpieServer = createMagpieServer();

        try {
            // Read GobPie configuration file
            GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, gobPieConfFileName);
            GobPieConfiguration gobpieConfiguration = gobPieConfReader.readGobPieConfiguration();

            // Start GoblintServer
            GoblintServer goblintServer = startGoblintServer(magpieServer, gobpieConfiguration);

            // Connect GoblintService and read configuration
            GoblintService goblintService = connectGoblintService(magpieServer, gobpieConfiguration, goblintServer);

            // Create file watcher for Goblint configuration
            GoblintConfWatcher goblintConfWatcher = getGoblintConfWatcher(magpieServer, goblintService, gobpieConfiguration);

            // Add analysis
            addAnalysis(magpieServer, gobpieConfiguration, goblintServer, goblintService, goblintConfWatcher);

            // Launch magpieServer
            magpieServer.configurationDone();
            log.info("MagpieBridge server launched.");

            if (args.length > 0 && gobpieConfiguration.enableAbstractDebugging()) {
                // Launch abstract debugging server
                String socketAddress = args[0];
                launchAbstractDebuggingServer(socketAddress, goblintService);
                log.info("Abstract debugging server launched on: " + socketAddress);
            } else {
                log.info("Abstract debugging server disabled.");
            }
        } catch (GobPieException e) {
            String message = e.getMessage();
            String terminalMessage;
            if (e.getCause() == null) terminalMessage = message;
            else terminalMessage = message + " Cause: " + e.getCause().getMessage();
            forwardErrorMessageToClient(magpieServer, message, terminalMessage);
        }

    }


    /**
     * Method for creating and launching MagpieBridge server.
     */

    private static GoblintMagpieServer createMagpieServer() {
        // set up configuration for MagpieServer
        GoblintServerConfiguration serverConfig = new GoblintServerConfiguration();
        serverConfig.setUseMagpieHTTPServer(false);
        //TODO: Track any relevant changes in https://github.com/MagpieBridge/MagpieBridge/issues/88 and update this accordingly.
        serverConfig.setLanguageExtensionHandler(new GoblintLanguageExtensionHandler(serverConfig.getLanguageExtensionHandler()));
        GoblintMagpieServer magpieServer = new GoblintMagpieServer(serverConfig);
        // launch MagpieServer
        // note that the server will not accept messages until configurationDone is called
        magpieServer.launchOnStdio();

        return magpieServer;
    }


    /**
     * Starts Goblint server.
     *
     * @throws GobPieException if running the server start command fails
     */
    public static GoblintServer startGoblintServer(MagpieServer magpieServer, GobPieConfiguration gobpieConfiguration) {
        GoblintServer goblintServer = new GoblintServer(magpieServer, gobpieConfiguration);
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

    private static GoblintConfWatcher getGoblintConfWatcher(GoblintMagpieServer magpieServer, GoblintService goblintService, GobPieConfiguration gobpieConfiguration) {
        FileWatcher fileWatcher = new FileWatcher(Path.of(gobpieConfiguration.getGoblintConf()));
        return new GoblintConfWatcher(magpieServer, goblintService, gobpieConfiguration, fileWatcher);
    }


    /**
     * Method for creating and adding Goblint analysis to MagpieBridge server.
     * <p>
     * Creates the GoblintAnalysis classes.
     */
    private static void addAnalysis(MagpieServer magpieServer, GobPieConfiguration gobpieConfiguration,
                                    GoblintServer goblintServer, GoblintService goblintService, GoblintConfWatcher goblintConfWatcher) {
        // define language
        String language = "c";

        // add analysis to the MagpieServer
        ServerAnalysis serverAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobpieConfiguration, goblintConfWatcher);
        magpieServer.addAnalysis(Either.forLeft(serverAnalysis), language);

        // add HTTP server for showing CFGs, only if the option is specified in the configuration
        if (gobpieConfiguration.showCfg()) {
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
    private static void launchAbstractDebuggingServer(String socketAddress, GoblintService goblintService) {
        ResultsService resultsService = new ResultsService(goblintService);
        AbstractDebuggingServerLauncher launcher = new AbstractDebuggingServerLauncher(resultsService);
        launcher.launchOnDomainSocket(socketAddress);
    }


    /**
     * Method for forwarding Error messages to MagpieServer.
     *
     * @param popUpMessage    The message shown on the pop-up message.
     * @param terminalMessage The message shown in the terminal.
     */

    private static void forwardErrorMessageToClient(MagpieServer magpieServer, String popUpMessage, String terminalMessage) {
        magpieServer.forwardMessageToClient(
                new MessageParams(MessageType.Error, "Unable to start GobPie extension: " + popUpMessage + " Check the output terminal of GobPie extension for more information.")
        );
        log.error(terminalMessage);
    }

}
