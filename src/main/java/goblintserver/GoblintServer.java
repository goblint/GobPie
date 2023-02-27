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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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
 * @author Juhan Oskar Hennoste
 * @since 0.0.2
 */

public class GoblintServer {

    private static final String GOBLINT_SOCKET = "goblint.sock";

    private final MagpieServer magpieServer;
    private final String[] goblintRunCommand;

    private StartedProcess goblintRunProcess;

    private final Logger log = LogManager.getLogger(GoblintServer.class);


    public GoblintServer(MagpieServer magpieServer) {
        this.magpieServer = magpieServer;
        this.goblintRunCommand = constructGoblintRunCommand();
    }

    public StartedProcess getGoblintRunProcess() {
        return goblintRunProcess;
    }

    public String getGoblintSocket() {
        return GOBLINT_SOCKET;
    }


    /**
     * Method for constructing the command to run Goblint server.
     * Files to analyse must be defined in goblint conf.
     */
    private String[] constructGoblintRunCommand() {
        return new String[]{
                "goblint",
                "--enable", "exp.arg",
                "--enable", "server.enabled",
                "--enable", "server.reparse",
                "--set", "server.mode", "unix",
                "--set", "server.unix-socket", new File(getGoblintSocket()).getAbsolutePath()
        };
    }


    private String[] constructGoblintVersionCheckCommand() {
        return new String[]{
                "goblint",
                "--version"
        };
    }


    /**
     * Method to start the Goblint server.
     *
     * @throws GobPieException when running Goblint fails.
     */
    public void startGoblintServer() {
        try {
            // run command to start goblint
            log.info("Goblint run with command: " + String.join(" ", goblintRunCommand));
            goblintRunProcess = runCommand(new File(System.getProperty("user.dir")), goblintRunCommand);
        } catch (IOException | InvalidExitValueException | InterruptedException | TimeoutException e) {
            throw new GobPieException("Running Goblint failed.", e, GOBLINT_EXCEPTION);
        }
    }


    /**
     * Checks Goblint command reported version.
     *
     * @throws GobPieException when running Goblint fails.
     */
    public String checkGoblintVersion() {
        File dirPath = new File(System.getProperty("user.dir"));
        String[] command = constructGoblintVersionCheckCommand();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            log.debug("Waiting for command: " + Arrays.toString(command) + " to run...");
            new ProcessExecutor()
                    .directory(dirPath)
                    .command(command)
                    .redirectOutput(output)
                    .redirectError(output)
                    .execute();
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new GobPieException("Checking version failed.", e, GOBLINT_EXCEPTION);
        }

        return output.toString();
    }


    /**
     * Method for running a command.
     *
     * @param dirPath The directory in which the command will run.
     * @param command The command to run.
     * @return An object that represents a process that has started. It may or may not have finished.
     */
    private StartedProcess runCommand(File dirPath, String[] command) throws IOException, InterruptedException, TimeoutException {
        ProcessListener listener = new ProcessListener() {

            public void afterStop(Process process) {
                if (process.exitValue() == 0) {
                    log.info("Goblint server has stopped.");
                } else if (process.exitValue() == 143) {
                    log.info("Goblint server has been killed.");
                } else {
                    magpieServer.forwardMessageToClient(new MessageParams(MessageType.Error, "Goblint server exited due to an error. Please check the output terminal of GobPie extension for more information."));
                    log.error("Goblint server exited due to an error (code: " + process.exitValue() + "). Please fix the issue reported above and rerun the analysis to restart the extension.");
                }
                magpieServer.cleanUp();
                // TODO: throw an exception? where (and how) can it be caught to be handled though?
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
