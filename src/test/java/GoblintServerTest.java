import goblintserver.GoblintServer;
import gobpie.GobPieConfReader;
import gobpie.GobPieConfiguration;
import gobpie.GobPieException;
import magpiebridge.core.MagpieServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.stream.SystemOut;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Goblint server test.
 * <p>
 * The class is responsible to ensure that @startGoblintServer
 * and @checkGoblintVersions functions execute correctly and informing
 * users of any errors encountered.
 *
 * @author Anette Taivere
 * @author Karoliine Holter
 */

@ExtendWith(SystemStubsExtension.class)
public class GoblintServerTest {

    @SystemStub
    private SystemOut systemOut;

    /**
     * Mock test to ensure @startGoblintServer function
     * runs given command
     */
    @Test
    public void testStartGoblintServer() {
        // Mock everything needed for creating goblintServer
        MagpieServer magpieServer = mock(MagpieServer.class);
        GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
        GoblintServer goblintServer = spy(new GoblintServer(magpieServer, gobPieConfiguration));

        // Mock behavior to return the constructed run command
        doReturn(List.of("sleep", "10s")).when(goblintServer).constructGoblintRunCommand();

        // Call startGoblintServer method
        goblintServer.startGoblintServer();

        // Assert that appropriate message is printed to the console
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Goblint run with command: ")));
    }

    /**
     * Mock test to ensure @startGoblintServer function
     * throws GobPieException when running Goblint fails.
     */
    @Test
    public void testStartGoblintServerFailed() {
        // Mock everything needed for creating goblintServer
        MagpieServer magpieServer = mock(MagpieServer.class);
        GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
        GoblintServer goblintServer = spy(new GoblintServer(magpieServer, gobPieConfiguration));

        // Mock behavior to return the executable command and abstract debugging
        when(gobPieConfiguration.goblintExecutable()).thenReturn("goblint");
        when(gobPieConfiguration.abstractDebugging()).thenReturn(true);

        // Assert that starting Goblint server throws GobPieException
        GobPieException thrown = assertThrows(GobPieException.class, goblintServer::startGoblintServer);
        assertEquals("Running Goblint failed.", thrown.getMessage());
    }


    /**
     * Mock test to ensure @checkGoblintVersion function
     * checks Goblint version
     */
    @Test
    public void testCheckGoblintVersion() {
        // Mock everything needed for creating goblintServer
        MagpieServer magpieServer = mock(MagpieServer.class);
        GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
        GoblintServer goblintServer = new GoblintServer(magpieServer, gobPieConfiguration);

        // Mock behavior to return the executable command
        when(gobPieConfiguration.goblintExecutable()).thenReturn("echo");

        // Call checkGoblintVersion method
        String output = goblintServer.checkGoblintVersion();

        // Assert that the output contains version information
        assertTrue(output.contains("version"));
        // Assert that appropriate messages are printed to the console
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Waiting for command: [echo, --version] to run...")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Executing [echo, --version]")));
    }

    /**
     * Mock test to ensure @checkGoblintVersion function
     * throws GobPieException when checking version fails.
     */
    @Test
    public void testCheckGoblintVersionFailed() {
        MagpieServer magpieServer = mock(MagpieServer.class);

        // Prepare test data
        String fileName = "gobpieTest7.json";
        String gobPieConfFileName = GobPieConfTest.class.getResource(fileName).getFile();

        // Mock everything needed for creating goblintServer
        GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, gobPieConfFileName);
        GobPieConfiguration gobPieConfiguration = gobPieConfReader.readGobPieConfiguration();
        GoblintServer goblintServer = new GoblintServer(magpieServer, gobPieConfiguration);

        // Assert that checking the Goblint version throws GobPieException
        GobPieException thrown = assertThrows(GobPieException.class, goblintServer::checkGoblintVersion);
        assertEquals("Checking version failed.", thrown.getMessage());
    }

}