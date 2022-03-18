package goblintserver;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.io.*;
import java.nio.file.*;

import com.google.gson.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.zeroturnaround.exec.*;
import org.zeroturnaround.exec.listener.ProcessListener;

import static goblintserver.GobPieExceptionType.*;

/**
 * The Class GoblintServer.
 * 
 * Reads the configuration for GobPie extension, including Goblint's configuration file name.
 * Starts Goblint Server and waits for the unix socket to be created.
 * 
 * @author      Karoliine Holter
 * @since       0.0.2
 */

public class GoblintServer {

    private final String gobPieConf;
    private final String goblintSocket = "goblint.sock";
    private String goblintConf;

    private String[] preAnalyzeCommand;
    private String[] goblintRunCommand;

    private StartedProcess goblintRunProcess;

    private final Logger log = LogManager.getLogger(GoblintClient.class);


    public GoblintServer(String gobPieConfFileName) {
        this.gobPieConf = gobPieConfFileName;
    }

    public String getGobPieConf() {
        return gobPieConf;
    }


    public String getGoblintConf() {
        return goblintConf;
    }


    public String[] getPreAnalyzeCommand() {
        return preAnalyzeCommand;
    }


    /**
     * Method to start the Goblint server.
     *
     * @throws GobPieException thrown when running Goblint failed
     */

    public void startGoblintServer() throws GobPieException {
        // read configuration file
        readGobPieConfiguration();

        try {
            // run command to start goblint
            log.info("Goblint run with command: " + String.join(" ", goblintRunCommand));

            goblintRunProcess = runCommand(new File(System.getProperty("user.dir")), goblintRunCommand);

            // wait until Goblint socket is created before continuing
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(System.getProperty("user.dir"));
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (((Path) event.context()).equals(Paths.get(goblintSocket))) {
                        log.info("Goblint server started.");
                        return;
                    }
                }
                key.reset();
            }
            
        } catch (IOException | InvalidExitValueException | InterruptedException | TimeoutException e) {
            throw new GobPieException("Running Goblint failed.", e, GOBLINT_EXCEPTION);
        }
    }


    /**
    * Method to stop the Goblint server.
    */

    public void stopGoblintServer() {
        goblintRunProcess.getProcess().destroy();
    }


    /**
     * Method to restart the Goblint server.
     *
     * @throws GobPieException thrown if starting Goblint Server throws an exception
     */

    public void restartGoblintServer() throws GobPieException {
        stopGoblintServer();
        startGoblintServer();
    }


    /**
     * Method for running a command.
     *
     * @param dirPath The directory in which the command will run.
     * @param command The command to run.
     * @return An object that represents a process that has started. It may or may not have finished.
     */

    public StartedProcess runCommand(File dirPath, String[] command) throws IOException, InterruptedException, InvalidExitValueException, TimeoutException {
        ProcessListener listener = new ProcessListener() {

            public void afterStop(Process process) {
                if (process.exitValue() == 0) {
                    log.info("Goblint server has stopped.");
                } else if (process.exitValue() == 143) {
                    log.info("Goblint server has been killed.");
                } else {
                    log.error("Goblint server exited due to an error. Please fix the issue reported above and restart the extension.");
                }
            }
        };
        
        log.debug("Waiting for command: " + command.toString() + " to run...");
        StartedProcess process = new ProcessExecutor()
                .directory(dirPath)
                .command(command)
                .redirectOutput(System.err)
                .redirectError(System.err)
                .addListener(listener)
                .start();
        return process;
    }


    /**
     * Method for reading GobPie configuration.
     * Deserializes json to GobPieConfiguration object.
     *
     * @throws GobPieException is thrown if
     *      * configuration parameters are missing (no goblint configuration file was specified);
     *      * gobpie conf cannot be found (no gobpie.json file is found in root directory)
     *      * gobpie conf's json syntax is wrong
     */

    private void readGobPieConfiguration() throws GobPieException {
        try {
            log.debug("Reading GobPie configuration from json");

            Gson gson = new GsonBuilder().create();
            // Read json object
            JsonObject jsonObject = JsonParser.parseReader(new FileReader(gobPieConf)).getAsJsonObject();

            // Convert json object to GobPieConfiguration object
            GobPieConfiguration gobpieConfiguration = gson.fromJson(jsonObject, GobPieConfiguration.class);
            this.preAnalyzeCommand = gobpieConfiguration.getPreAnalyzeCommand();
            this.goblintConf = gobpieConfiguration.getGoblintConf();

            // Check if all required parameters have been set
            if (goblintConf.equals("")) {
                throw new GobPieException("Configuration parameters missing from GobPie configuration file.", GOBPIE_CONF_EXCEPTION);
            }

            // Construct command to run Goblint Server 
            // Files to analyse must be defined in goblint conf
            this.goblintRunCommand = Arrays.stream(new String[]{
                                    "goblint", "--conf", new File(goblintConf).getAbsolutePath(),
                                    "--enable", "server.enabled",
                                    "--enable", "server.reparse",
                                    "--set", "server.mode", "unix",
                                    "--set", "server.unix-socket", new File(goblintSocket).getAbsolutePath()})
                    .toArray(String[]::new);

            log.debug("GobPie configuration read from json");
        } catch (FileNotFoundException e) {
            throw new GobPieException("Could not locate GobPie configuration file.", e, GOBPIE_CONF_EXCEPTION);
        } catch (JsonSyntaxException e) {
            throw new GobPieException("Gobpie configuration file syntax is wrong.", e, GOBPIE_CONF_EXCEPTION);
        }
    }

}
