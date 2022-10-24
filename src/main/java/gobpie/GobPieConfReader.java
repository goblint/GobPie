package gobpie;

import com.google.gson.*;
import magpiebridge.core.MagpieServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;

public class GobPieConfReader {

    private final MagpieServer magpieServer;
    private final String gobPieConfFileName;
    private static final Logger log = LogManager.getLogger(GobPieConfReader.class);

    public GobPieConfReader(MagpieServer magpieServer, String gobPieConfFileName) {
        this.magpieServer = magpieServer;
        this.gobPieConfFileName = gobPieConfFileName;
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
    public GobPieConfiguration readGobPieConfiguration() {

        // If gobpie configuration is not present, wait for it to be created
        if (!new File(System.getProperty("user.dir") + "/" + gobPieConfFileName).exists()) {
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
     * Method for waiting until GobPie configuration file is created or modified to satisfy the requirements.
     */

    private void waitForGobPieConf() {

        // wait until GobPie configuration file is created before continuing
        WatchService watchService;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(System.getProperty("user.dir"));
            path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if ((event.context()).equals(Paths.get(gobPieConfFileName))) {
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

    private GobPieConfiguration parseGobPieConf() {
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
            throw new GobPieException("GobPie configuration file syntax is wrong.", e, GobPieExceptionType.GOBPIE_CONF_EXCEPTION);
        }
    }

    /**
     * Method for forwarding Error messages to MagpieServer.
     *
     * @param popUpMessage    The message shown on the pop-up message.
     * @param terminalMessage The message shown in the terminal.
     */

    private void forwardErrorMessageToClient(String popUpMessage, String terminalMessage) {
        magpieServer.forwardMessageToClient(
                new MessageParams(MessageType.Error, "Unable to start GobPie extension: " + popUpMessage + " Please check the output terminal of GobPie extension for more information.")
        );
        log.error(terminalMessage);
    }

}
