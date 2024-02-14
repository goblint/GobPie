import gobpie.GobPieConfReader;
import gobpie.GobPieConfiguration;
import magpiebridge.core.MagpieServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.stream.SystemOut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(SystemStubsExtension.class)
class GobPieConfTest {

    @SystemStub
    private SystemOut systemOut;

    @Test
    void testReadGobPieConfiguration() {
        // Mock everything needed for creating GobPieConfReader
        MagpieServer magpieServer = mock(MagpieServer.class);
        String gobPieConfFileName = GobPieConfTest.class.getResource("gobpie.json").getFile();

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

}
