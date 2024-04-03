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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        MagpieServer magpieServer = mock(MagpieServer.class);
        GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
        GoblintServer goblintServer = spy(new GoblintServer(magpieServer, gobPieConfiguration));

        doReturn(new String[]{"sleep", "10s"}).when(goblintServer).constructGoblintRunCommand();
        goblintServer.startGoblintServer();
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Goblint run with command: ")));
    }

    /**
     * Mock test to ensure @startGoblintServer function
     * throws GobPieException when running Goblint fails.
     */
    @Test
    public void testStartGoblintServerFailed() {
        MagpieServer magpieServer = mock(MagpieServer.class);
        GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
        GoblintServer goblintServer = spy(new GoblintServer(magpieServer, gobPieConfiguration));

        when(gobPieConfiguration.getGoblintExecutable()).thenReturn("goblint");
        when(gobPieConfiguration.enableAbstractDebugging()).thenReturn(true);

        GobPieException thrown = assertThrows(GobPieException.class, goblintServer::startGoblintServer);
        assertEquals("Running Goblint failed.", thrown.getMessage());
    }


    /**
     * Mock test to ensure @checkGoblintVersion function
     * checks Goblint version
     */
    @Test
    public void testCheckGoblintVersion() {
        MagpieServer magpieServer = mock(MagpieServer.class);
        GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
        GoblintServer goblintServer = new GoblintServer(magpieServer, gobPieConfiguration);

        when(gobPieConfiguration.getGoblintExecutable()).thenReturn("echo");

        String output = goblintServer.checkGoblintVersion();

        assertTrue(output.contains("version"));
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
        String fileName = "gobpieTest7.json";
        String gobPieConfFileName = GobPieConfTest.class.getResource(fileName).getFile();
        GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, gobPieConfFileName);
        GobPieConfiguration gobPieConfiguration = gobPieConfReader.readGobPieConfiguration();
        GoblintServer goblintServer = new GoblintServer(magpieServer, gobPieConfiguration);
        GobPieException thrown = assertThrows(GobPieException.class, goblintServer::checkGoblintVersion);
        assertEquals("Checking version failed.", thrown.getMessage());
    }

}
