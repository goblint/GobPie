package gobpie;

import com.google.gson.*;
import magpiebridge.core.MagpieServer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import util.FileWatcher;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The Class GobPieConfReader.
 * <p>
 * Class for parsing and reading GobPie configuration file.
 *
 * @author Karoliine Holter
 * @author Juhan Oskar Hennoste
 * @since 0.0.2
 */

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

        // If GobPie configuration is not present, wait for it to be created
        Path gobPieConfPath = Path.of(gobPieConfFileName);
        try (FileWatcher gobPieConfWatcher = new FileWatcher(gobPieConfPath)) {
            if (!Files.exists(gobPieConfPath)) {
                String message = "GobPie configuration file is not found in the project root.";
                String terminalMessage = message + "\nPlease add GobPie configuration file into the project root.";
                forwardErrorMessageToClient(message, terminalMessage);
                gobPieConfWatcher.waitForModified();
            }

            // Parse the configuration file
            GobPieConfiguration gobpieConfiguration = parseGobPieConf();

            // Check if all required parameters have been set
            // If not, wait for change and reparse
            while (gobpieConfiguration.getGoblintConf() == null || gobpieConfiguration.getGoblintConf().equals("")) {
                String message = "goblintConf parameter missing from GobPie configuration file.";
                String terminalMessage = message + "\nPlease add Goblint configuration file location into GobPie configuration as a parameter with name \"goblintConf\".";
                forwardErrorMessageToClient(message, terminalMessage);
                gobPieConfWatcher.waitForModified();
                gobpieConfiguration = parseGobPieConf();
            }

            return gobpieConfiguration;
        } catch (InterruptedException e) {
            return ExceptionUtils.rethrow(e);
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
