import analysis.GoblintAnalysis;
import api.GoblintService;
import api.messages.GoblintAnalysisResult;
import api.messages.params.AnalyzeParams;
import api.messages.params.Params;
import com.ibm.wala.classLoader.Module;
import goblintserver.GoblintConfWatcher;
import goblintserver.GoblintServer;
import gobpie.GobPieConfiguration;
import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.MagpieServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.stream.SystemOut;
import util.FileWatcher;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Goblint configuration test.
 * <p>
 * The class is responsible for testing configuration settings.
 * It ensures proper functionality of the settings and handles
 * any errors that may occur during the process.
 *
 * @author Anette Taivere
 * @author Karoliine Holter
 */

@ExtendWith(SystemStubsExtension.class)
public class GoblintConfTest {

    @SystemStub
    private SystemOut systemOut;

    @Mock
    MagpieServer magpieServer = mock(MagpieServer.class);
    @Mock
    GoblintService goblintService = mock(GoblintService.class);
    @Mock
    GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
    @Spy
    GoblintServer goblintServer = spy(new GoblintServer(magpieServer, gobPieConfiguration));
    // Mock the arguments (files and analysisConsumer) for calling the GoblintAnalyze.analyze method
    Collection<? extends Module> files = new ArrayDeque<>();
    AnalysisConsumer analysisConsumer = mock(AnalysisConsumer.class);

    /**
     * Mock test to ensure refreshing
     * the Goblint configuration succeeds.
     */
    @Test
    void refreshGoblintConfigSucceeds() {
        // Simulate Goblint server as alive
        doReturn(true).when(goblintServer).isAlive();

        // Mock GobPieConfiguration methods to return specific values
        when(gobPieConfiguration.preAnalyzeCommand()).thenReturn(new ArrayList<>());
        when(gobPieConfiguration.incrementalAnalysis()).thenReturn(true);
        when(gobPieConfiguration.goblintConf()).thenReturn("goblint.json");

        // Mock GoblintService methods to return successful analysis results and config operations
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult(List.of("Success"))));
        when(goblintService.reset_config()).thenReturn(CompletableFuture.completedFuture(null));
        when(goblintService.read_config(new Params(new File("goblint.json").getAbsolutePath()))).thenReturn(CompletableFuture.completedFuture(null));

        // Create instances of GoblintConfWatcher and GoblintAnalysis
        GoblintConfWatcher goblintConfWatcher = new GoblintConfWatcher(magpieServer, goblintService, gobPieConfiguration, new FileWatcher(Path.of("")));
        GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfiguration, goblintConfWatcher);

        // Call analyze method to trigger configuration refresh
        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Assert that configValid is true after configuration refresh
        assertTrue(goblintConfWatcher.configValid);

        goblintAnalysis.analyze(files, analysisConsumer, true);
        assertTrue(goblintConfWatcher.configValid);
    }

    /**
     * Mock test to ensure @analyse function
     * messages user when Goblint configuration refresh process fails
     */
    @Test
    void refreshGoblintConfigFails() {
        // Simulate Goblint server as alive
        doReturn(true).when(goblintServer).isAlive();

        // Mock GobPieConfiguration methods to return specific values
        when(gobPieConfiguration.preAnalyzeCommand()).thenReturn(new ArrayList<>());
        when(gobPieConfiguration.incrementalAnalysis()).thenReturn(true);
        when(gobPieConfiguration.goblintConf()).thenReturn("goblint.json");

        // Mock GoblintService methods to return successful analysis results and config operations
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult(List.of("Success"))));
        when(goblintService.reset_config()).thenReturn(CompletableFuture.completedFuture(null));
        when(goblintService.read_config(new Params())).thenReturn(CompletableFuture.completedFuture(null));

        // Mock FileWatcher to simulate modification in the file
        FileWatcher fileWatcher = spy(new FileWatcher(Path.of("")));
        when(fileWatcher.checkModified()).thenReturn(true);

        // Create instances of GoblintConfWatcher and GoblintAnalysis
        GoblintConfWatcher goblintConfWatcher = new GoblintConfWatcher(magpieServer, goblintService, gobPieConfiguration, fileWatcher);
        GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfiguration, goblintConfWatcher);

        // Call analyze method to trigger configuration refresh
        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Assert that configValid is false after configuration refresh fails
        assertFalse(goblintConfWatcher.configValid);

        // Assert that the appropriate error message is printed
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Goblint was unable to successfully read the new configuration: ")));
    }

}