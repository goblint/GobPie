import com.google.gson.JsonSyntaxException;
import gobpie.GobPieConfReader;
import gobpie.GobPieConfiguration;
import gobpie.GobPieException;
import magpiebridge.core.MagpieServer;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.stream.SystemOut;
import util.FileWatcher;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
class GobPieConfTest {

    @SystemStub
    private SystemOut systemOut;

    @Test
    void testReadGobPieConfiguration() {
        // Mock everything needed for creating GobPieConfReader
        MagpieServer magpieServer = mock(MagpieServer.class);
        String gobPieConfFileName = GobPieConfTest.class.getResource("gobpieTest1.json").getFile();
        GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, gobPieConfFileName);

        GobPieConfiguration expectedGobPieConfiguration =
                new GobPieConfiguration.Builder()
                        .setGoblintConf("goblint.json")
                        .setGoblintExecutable("/home/user/goblint/analyzer/goblint")
                        .setPreAnalyzeCommand(new String[]{"echo", "'hello'"})
                        .setAbstractDebugging(true)
                        .setShowCfg(true)
                        .setIncrementalAnalysis(false)
                        .setExplodeGroupWarnings(true)
                        .createGobPieConfiguration();

        GobPieConfiguration actualGobPieConfiguration = gobPieConfReader.readGobPieConfiguration();

        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Reading GobPie configuration from json")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("GobPie configuration read from json")));
        assertEquals(expectedGobPieConfiguration, actualGobPieConfiguration);
    }


    @Test
    void testReadCompleteGobPieConfiguration() {
        // Mock everything needed for creating GobPieConfReader
        MagpieServer magpieServer = mock(MagpieServer.class);
        String gobPieConfFileName = GobPieConfTest.class.getResource("gobpieTest2.json").getFile();
        GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, gobPieConfFileName);

        GobPieConfiguration expectedGobPieConfiguration =
                new GobPieConfiguration.Builder()
                        .setGoblintConf("goblint.json")
                        .setGoblintExecutable("/home/user/goblint/analyzer/goblint")
                        .setPreAnalyzeCommand(new String[]{"cmake", "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON", "-B", "build"})
                        .setAbstractDebugging(false)
                        .setShowCfg(false)
                        .setIncrementalAnalysis(true)
                        .setExplodeGroupWarnings(false)
                        .createGobPieConfiguration();

        GobPieConfiguration actualGobPieConfiguration = gobPieConfReader.readGobPieConfiguration();

        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Reading GobPie configuration from json")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("GobPie configuration read from json")));
        assertEquals(expectedGobPieConfiguration, actualGobPieConfiguration);
    }

    @Test
    void testGobPieConfigurationFailMissingInRoot() throws InterruptedException, IOException {
        // Mock everything needed for creating GobPieConfReader
        MagpieServer magpieServer = mock(MagpieServer.class);
        FileWatcher gobPieConfWatcher = mock(FileWatcher.class);
        String gobPieConfFileName =  GobPieConfTest.class.getResource("gobpie.json").getFile();

        GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, gobPieConfFileName);
        doNothing().when(gobPieConfWatcher).waitForModified();

        GobPieConfiguration actualGobPieConfiguration = gobPieConfReader.readGobPieConfiguration();
        //.waitForModified() juurde jääb kinni
        
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Reading GobPie configuration from json")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("GobPie configuration file is not found in the project root.")));
    }


    @Test
    void testReadGobPieConfigurationWithExtraField() {
        // Mock everything needed for creating GobPieConfReader
        MagpieServer magpieServer = mock(MagpieServer.class);
        String gobPieConfFileName = GobPieConfTest.class.getResource("gobpieTest4.json").getFile();
        GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, gobPieConfFileName);

        GobPieConfiguration expectedGobPieConfiguration =
                new GobPieConfiguration.Builder()
                        .setGoblintConf("goblint.json")
                        .setGoblintExecutable("/home/user/goblint/analyzer/goblint")
                        .setPreAnalyzeCommand(new String[]{"cmake", "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON", "-B", "build"})
                        .setAbstractDebugging(false)
                        .setShowCfg(false)
                        .setIncrementalAnalysis(true)
                        .setExplodeGroupWarnings(false)
                        .createGobPieConfiguration();

        GobPieConfiguration actualGobPieConfiguration = gobPieConfReader.readGobPieConfiguration();

        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Reading GobPie configuration from json")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("GobPie configuration read from json")));
        assertEquals(expectedGobPieConfiguration, actualGobPieConfiguration);
    }

    @Test
    void testReadGobPieConfigurationWithWrongJSONSyntax() {
        // Mock everything needed for creating GobPieConfReader
        MagpieServer magpieServer = mock(MagpieServer.class);
        String gobPieConfFileName = GobPieConfTest.class.getResource("gobpieTest5.json").getFile();
        GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, gobPieConfFileName);

        GobPieException thrown = assertThrows(GobPieException.class, gobPieConfReader::readGobPieConfiguration);
        assertEquals("GobPie configuration file syntax is wrong.", thrown.getMessage());


    @Test
    void testGobPieConfigurationWithoutGoblintConfField() {
        // Mock everything needed for creating GobPieConfReader
        MagpieServer magpieServer = mock(MagpieServer.class);
        String gobPieConfFileName = GobPieConfTest.class.getResource("gobpieTest6.json").getFile();
        GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, gobPieConfFileName);

        GobPieConfiguration actualGobPieConfiguration = gobPieConfReader.readGobPieConfiguration();
        //.waitForModified() juurde jääb kinni

        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Reading GobPie configuration from json")));
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Error, "goblintConf parameter missing from GobPie configuration file."));
    }

    @Test
    void testGobPieConfigurationDefaultValues() {
        // Mock everything needed for creating GobPieConfReader
        MagpieServer magpieServer = mock(MagpieServer.class);
        String gobPieConfFileName = GobPieConfTest.class.getResource("gobpieTest7.json").getFile();
        GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, gobPieConfFileName);

        GobPieConfiguration expectedGobPieConfiguration =
                new GobPieConfiguration.Builder()
                        .setGoblintConf("goblint.json")
                        .setGoblintExecutable("goblint")
                        .setPreAnalyzeCommand(null)
                        .setAbstractDebugging(false)
                        .setShowCfg(false)
                        .setIncrementalAnalysis(true)
                        .setExplodeGroupWarnings(true)
                        .createGobPieConfiguration();

        GobPieConfiguration actualGobPieConfiguration = gobPieConfReader.readGobPieConfiguration();

        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Reading GobPie configuration from json")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("GobPie configuration read from json")));
        assertEquals(expectedGobPieConfiguration, actualGobPieConfiguration);
    }


}
