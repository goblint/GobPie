import goblintserver.GoblintServer;
import gobpie.GobPieConfReader;
import gobpie.GobPieConfiguration;
import gobpie.GobPieException;
import magpiebridge.core.MagpieServer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.stream.SystemOut;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GoblintServerTest {

    @SystemStub
    private SystemOut systemOut;


    @Test
    public void testStartGoblintServer() {
        /**

        GoblintServer goblintServer = mock(GoblintServer.class);
        MagpieServer magpieServer = mock(MagpieServer.class);
        //GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);

        String fileName = "gobpieTest7.json";
        String gobPieConfFileName = GobPieConfTest.class.getResource(fileName).getFile();
        GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, gobPieConfFileName);

        GobPieConfiguration gobPieConfiguration = gobPieConfReader.readGobPieConfiguration();
        Logger log = mock(Logger.class);
        //doNothing().when(log).isDebugEnabled();
        //doAnswer().when(log).isDebugEnabled();
        doReturn(false).when(log).isDebugEnabled();
        //when(log.isDebugEnabled()).thenReturn(false);

        Main.startGoblintServer(magpieServer, gobPieConfiguration);

        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Goblint run with command: ")));

        **/
    }


    @Test
    public void testCheckGoblintVersion() {
        /**

        MagpieServer magpieServer = mock(MagpieServer.class);
        String fileName = "gobpieTest7.json";
        String gobPieConfFileName = GobPieConfTest.class.getResource(fileName).getFile();
        GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, gobPieConfFileName);
        GobPieConfiguration gobPieConfiguration = gobPieConfReader.readGobPieConfiguration();
        GoblintServer goblintServer = new GoblintServer(magpieServer, gobPieConfiguration);
        Logger log = mock(Logger.class);

        when(log.isDebugEnabled()).thenReturn(false);

        Main.startGoblintServer(magpieServer, gobPieConfiguration);

        assertTrue(systemOut.getLines().anyMatch(line -> line.contains(" Waiting for command: ")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Executing ")));

        **/
    }


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
