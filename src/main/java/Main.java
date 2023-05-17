import HTTPserver.GobPieHTTPServer;
import analysis.GoblintAnalysis;
import analysis.ShowCFGCommand;
import api.GoblintService;
import api.GoblintServiceLauncher;
import api.messages.Params;
import goblintserver.GoblintServer;
import gobpie.GobPieConfReader;
import gobpie.GobPieConfiguration;
import gobpie.GobPieException;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.ToolAnalysis;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.io.File;

public class Main {

    private static final String gobPieConfFileName = "gobpie.json";
    private static final Logger log = LogManager.getLogger(Main.class);
    private static GoblintMagpieServer magpieServer;

    public static void main(String... args) {

        try {
            createMagpieServer();
            addAnalysis();
            magpieServer.configurationDone();
            log.info("MagpieBridge server launched.");
        } catch (GobPieException e) {
            String message = e.getMessage();
            String terminalMessage = e.getCause() == null ? message : message + " Cause: " + e.getCause().getMessage();
            forwardErrorMessageToClient(message, terminalMessage);
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

    private static void createMagpieServer() {
        // set up configuration for MagpieServer
        GoblintServerConfiguration serverConfig = new GoblintServerConfiguration();
        serverConfig.setUseMagpieHTTPServer(false);
        //TODO: Track any relevant changes in https://github.com/MagpieBridge/MagpieBridge/issues/88 and update this accordingly.
        serverConfig.setLanguageExtensionHandler(new GoblintLanguageExtensionHandler(serverConfig.getLanguageExtensionHandler()));
        magpieServer = new GoblintMagpieServer(serverConfig);
        // launch MagpieServer
        // note that the server will not accept messages until configurationDone is called
        magpieServer.launchOnStdio();
    }


    /**
     * Method for creating and adding Goblint analysis to MagpieBridge server.
     * <p>
     * Creates GoblintServer, GoblintClient and the GoblintAnalysis classes.
     *
     * @throws GobPieException if something goes wrong with creating any of the classes:
     *                         <ul>
     *                             <li>GoblintServer;</li>
     *                             <li>GoblintClient.</li>
     *                         </ul>
     */

    private static void addAnalysis() {
        // define language
        String language = "c";

        // read GobPie configuration file
        GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, gobPieConfFileName);
        GobPieConfiguration gobpieConfiguration = gobPieConfReader.readGobPieConfiguration();

        // start GoblintServer
        GoblintServer goblintServer = new GoblintServer(magpieServer, gobpieConfiguration);
        goblintServer.startGoblintServer();

        // launch GoblintService
        GoblintServiceLauncher launcher = new GoblintServiceLauncher();
        GoblintService goblintService = launcher.connect(goblintServer.getGoblintSocket());

        // read Goblint configurations
        goblintService.read_config(new Params(new File(gobpieConfiguration.getGoblintConf()).getAbsolutePath()))
                .exceptionally(ex -> {
                    String msg = "Goblint was unable to successfully read the configuration: " + ex.getMessage();
                    magpieServer.forwardMessageToClient(new MessageParams(MessageType.Warning, msg));
                    log.error(msg);
                    return null;
                })
                .join();

        // add analysis to the MagpieServer
        ServerAnalysis serverAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobpieConfiguration);
        Either<ServerAnalysis, ToolAnalysis> analysis = Either.forLeft(serverAnalysis);
        magpieServer.addAnalysis(analysis, language);

        // add HTTP server for showing CFGs, only if the option is specified in the configuration
        if (gobpieConfiguration.showCfg()) {
            String httpServerAddress = new GobPieHTTPServer(goblintService).start();
            magpieServer.addHttpServer(httpServerAddress);
            magpieServer.addCommand("showcfg", new ShowCFGCommand(httpServerAddress));
        }
    }


    /**
     * Method for forwarding Error messages to MagpieServer.
     *
     * @param popUpMessage    The message shown on the pop-up message.
     * @param terminalMessage The message shown in the terminal.
     */

    private static void forwardErrorMessageToClient(String popUpMessage, String terminalMessage) {
        magpieServer.forwardMessageToClient(
                new MessageParams(MessageType.Error, "Unable to start GobPie extension: " + popUpMessage + " Check the output terminal of GobPie extension for more information.")
        );
        log.error(terminalMessage);
    }

}
