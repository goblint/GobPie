package goblintserver;

import gobpie.GobPieException;
import magpiebridge.core.MagpieServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.listener.ProcessListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import static gobpie.GobPieExceptionType.GOBLINT_EXCEPTION;

/**
 * The Class GoblintServer.
 * <p>
 * Reads the configuration for GobPie extension, including Goblint configuration file name.
 * Starts Goblint Server and waits for the unix socket to be created.
 *
 * @author Karoliine Holter
 * @since 0.0.2
 */

public class GoblintServer {

    private final String goblintSocket = "goblint.sock";
    private final String goblintConf;
    private final MagpieServer magpieServer;
    private final String[] goblintRunCommand;

    private StartedProcess goblintRunProcess;

    private final Logger log = LogManager.getLogger(GoblintServer.class);


    public GoblintServer(String goblintConfName, MagpieServer magpieServer) {
        this.goblintConf = goblintConfName;
        this.magpieServer = magpieServer;
        this.goblintRunCommand = constructGoblintRunCommand();
    }


    public String getGoblintConf() {
        return goblintConf;
    }

    public StartedProcess getGoblintRunProcess() {
        return goblintRunProcess;
    }


    /**
     * Method for constructing the command to run Goblint server.
     * Files to analyse must be defined in goblint conf.
     *
     * @throws GobPieException when running Goblint failed.
     */

    private String[] constructGoblintRunCommand() {
        return Arrays.stream(new String[]{
                        "goblint",
                        "--enable", "server.enabled",
                        "--enable", "server.reparse",
                        "--set", "server.mode", "unix",
                        "--set", "server.unix-socket", new File(goblintSocket).getAbsolutePath()})
                .toArray(String[]::new);
    }


    /**
     * Method to start the Goblint server.
     *
     * @throws GobPieException when running Goblint fails.
     */

    public void startGoblintServer() {

        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(System.getProperty("user.dir"));
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            WatchKey key;

            // run command to start goblint
            log.info("Goblint run with command: " + String.join(" ", goblintRunCommand));

            goblintRunProcess = runCommand(new File(System.getProperty("user.dir")), goblintRunCommand);

            // wait until Goblint socket is created before continuing
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if ((event.context()).equals(Paths.get(goblintSocket))) {
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
                    magpieServer.forwardMessageToClient(new MessageParams(MessageType.Error, "Goblint server exited due to an error. Please check the output terminal of GobPie extension for more information."));
                    log.error("Goblint server exited due to an error (code: " + process.exitValue() + "). Please fix the issue reported above and restart the extension.");
                }
            }
        };

        log.debug("Waiting for command: " + Arrays.toString(command) + " to run...");
        return new ProcessExecutor()
                .directory(dirPath)
                .command(command)
                .redirectOutput(System.err)
                .redirectError(System.err)
                .addListener(listener)
                .start();
    }

}
