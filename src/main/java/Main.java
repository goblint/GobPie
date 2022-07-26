import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;

import analysis.ShowCFGCommand;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.ServerConfiguration;
import magpiebridge.core.ToolAnalysis;

import com.google.gson.*;

import analysis.GoblintAnalysis;
import goblintclient.GoblintClient;
import goblintserver.GoblintServer;
import gobpie.GobPieConfiguration;
import gobpie.GobPieException;
import gobpie.GobPieExceptionType;

import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static final String gobPieConfFileName = "gobpie.json";
    private static final String cfgHttpServer = "http://localhost:8080/";
    private static final Logger log = LogManager.getLogger(Main.class);
    private static MagpieServer magpieServer;

    public static void main(String... args) {

        try {
            createMagpieServer();
            addAnalysis();
            // launch magpieServer
            magpieServer.launchOnStdio();
            log.info("MagpieBridge server launched.");
            magpieServer.doAnalysis("c", true);
        } catch (GobPieException e) {
            String message = e.getMessage();
            String terminalMessage;
            if (e.getCause() == null) terminalMessage = message;
            else terminalMessage = message + " Cause: " + e.getCause().getMessage();
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
        ServerConfiguration serverConfig = new ServerConfiguration();
        serverConfig.setDoAnalysisByFirstOpen(false);
        magpieServer = new MagpieServer(serverConfig);
    }


    /**
     * Method for creating and adding GoblintAnalysis to MagpieBridge server
     * and doing the initial analysis.
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

        // read gobpie configuration file
        GobPieConfiguration gobpieConfiguration = readGobPieConfiguration();

        // start GoblintServer
        GoblintServer goblintServer = new GoblintServer(gobpieConfiguration.getGoblintConf(), magpieServer);
        goblintServer.startGoblintServer();

        // connect GoblintClient
        GoblintClient goblintClient = new GoblintClient();
        goblintClient.connectGoblintClient();

        // add analysis to the MagpieServer
        ServerAnalysis serverAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintClient, gobpieConfiguration);
        Either<ServerAnalysis, ToolAnalysis> analysis = Either.forLeft(serverAnalysis);
        magpieServer.addAnalysis(analysis, language);

        // TODO: move into separate function?
        magpieServer.addHttpServer(cfgHttpServer);
        magpieServer.addCommand("showcfg", new ShowCFGCommand(goblintClient));
    }


    /**
     * Method for reading GobPie configuration.
     * <p>
     * Waits for the Gobpie configuration file to be created if one is not present in the project root.
     * Checks if all the required parameters are present in the configuration and
     * if not, waits for the file to be changed and reparses it until the user gives the parameters.
     *
     * @return GobPieConfiguration object.
     */

    private static GobPieConfiguration readGobPieConfiguration() {

        // If gobpie configuration is not present, wait for it to be created
        if (!new File(System.getProperty("user.dir") + "/" + "gobpie.json").exists()) {
            String message = "GobPie configuration file is not found in the project root.";
            String terminalMessage = message + "\nPlease add GobPie configuration file into the project root.";
            forwardErrorMessageToClient(message, terminalMessage);
            waitForGobPieConf();
        }

        // Parse the configuration file
        GobPieConfiguration gobpieConfiguration = parseGobPieConf();

        // Check if all required parameters have been set
        // If not, wait for change and reparse
        String goblintConf = gobpieConfiguration.getGoblintConf();
        while (goblintConf == null || goblintConf.equals("")) {
            String message = "goblintConf parameter missing from GobPie configuration file.";
            String terminalMessage = message + "\nPlease add Goblint configuration file location into GobPie configuration as a parameter with name \"goblintConf\".";
            forwardErrorMessageToClient(message, terminalMessage);
            waitForGobPieConf();
            gobpieConfiguration = parseGobPieConf();
            goblintConf = gobpieConfiguration.getGoblintConf();
        }

        return gobpieConfiguration;


    }


    /**
     * Method for parsing GobPie configuration.
     * Deserializes json to GobPieConfiguration object.
     *
     * @return GobPieConfiguration object.
     * @throws GobPieException if
     *                         <ul>
     *                             <li>gobpie conf cannot be found to be read from;</li>
     *                             <li>gobpie conf json syntax is wrong.</li>
     *                         </ul>
     */

    private static GobPieConfiguration parseGobPieConf() {
        try {
            log.debug("Reading GobPie configuration from json");
            Gson gson = new GsonBuilder().create();
            // Read json object
            JsonObject jsonObject = JsonParser.parseReader(new FileReader(gobPieConfFileName)).getAsJsonObject();
            // Convert json object to GobPieConfiguration object
            log.debug("GobPie configuration read from json");
            return gson.fromJson(jsonObject, GobPieConfiguration.class);
        } catch (FileNotFoundException e) {
            throw new GobPieException("Could not locate GobPie configuration file.", e, GobPieExceptionType.GOBPIE_CONF_EXCEPTION);
        } catch (JsonSyntaxException e) {
            throw new GobPieException("Gobpie configuration file syntax is wrong.", e, GobPieExceptionType.GOBPIE_CONF_EXCEPTION);
        }
    }


    /**
     * Method for waiting until GobPie configuration file is created or modified to satisfy the requirements.
     */

    private static void waitForGobPieConf() {

        // wait until GobPie configuration file is created before continuing
        WatchService watchService;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(System.getProperty("user.dir"));
            path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (((Path) event.context()).equals(Paths.get(gobPieConfFileName))) {
                        return;
                    }
                }
                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            String message = "Waiting for GobPie configuration file failed.";
            forwardErrorMessageToClient(message, message + e.getMessage());
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
                new MessageParams(MessageType.Error, "Unable to start GobPie extension: " + popUpMessage + " Please check the output terminal of GobPie extension for more information.")
        );
        log.error(terminalMessage);
    }

}
