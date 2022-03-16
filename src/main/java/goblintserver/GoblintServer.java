package goblintserver;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.io.*;
import java.nio.file.*;

import com.google.gson.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.zeroturnaround.exec.*;
import org.zeroturnaround.exec.listener.ProcessListener;

import magpiebridge.core.MagpieServer;


public class GoblintServer {

    private MagpieServer magpieServer;

    private final String gobPieConf;
    private final String goblintSocket = "goblint.sock";
    private String goblintConf;

    private String[] preAnalyzeCommand;
    private String[] goblintRunCommand;

    private StartedProcess goblintRunProcess;

    private final Logger log = LogManager.getLogger(GoblintClient.class);


    public GoblintServer(MagpieServer magpieServer, String gobPieConfFileName) {
        this.magpieServer = magpieServer;
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
     * @return True if server was started successfully, false otherwise.
     */

    public boolean startGoblintServer() {
        // read configuration file
        boolean gobpieconf = readGobPieConfiguration();
        if (!gobpieconf) return false;

        try {
            // run command to start goblint
            log.info("Goblint run with command: " + String.join(" ", goblintRunCommand));

            goblintRunProcess = runCommand(new File(System.getProperty("user.dir")), goblintRunCommand);

            if (goblintRunProcess.getFuture().isDone() && goblintRunProcess.getProcess().exitValue() != 0) {
                magpieServer.forwardMessageToClient(new MessageParams(MessageType.Error, "Goblint exited with an error."));
                log.error("Goblint exited with an error.");
                return false;
            }

            // wait until Goblint socket is created before continuing
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(System.getProperty("user.dir"));
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE );
            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    
                    if (((Path) event.context()).equals(Paths.get(goblintSocket))) {
                        log.info("Goblint server started.");
                        return true;
                    }
                }
                key.reset();
            }

            return false;
            
        } catch (IOException | InvalidExitValueException | InterruptedException | TimeoutException e) {
            log.error("Running Goblint failed. " + e.getMessage());
            return false;
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
     * @return True if new server was started successfully, false otherwise.
     */

    public boolean restartGoblintServer() {
        stopGoblintServer();
        return startGoblintServer();
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
                    magpieServer.forwardMessageToClient(new MessageParams(MessageType.Info, "Goblint server has stopped."));
                    log.info("Goblint server has stopped.");
                } else if (process.exitValue() == 143) {
                    log.info("Goblint server has been killed.");
                } else {
                    magpieServer.forwardMessageToClient(new MessageParams(MessageType.Error, "Goblint server exited due to an error. Please check the output terminal of GobPie extension for more information."));
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
     * @return true if gobpie configuration was read sucessfully, false otherwise:
     *      * no goblint configuration file was specified;
     *      * no files to analyse have been listed;
     *      * no gobpie.json file is found in root directory
     */

    private boolean readGobPieConfiguration() {
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
                log.error("Configuration parameters missing from GobPie configuration file.");
                return false;
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
            log.error("Could not locate GobPie configuration file. " + e.getMessage());
            return false;
        }
        return true;
    }

}
