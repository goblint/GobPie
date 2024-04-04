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
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Spy;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.stream.SystemOut;
import util.FileWatcher;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

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

    @Test
    void refreshGoblintConfigSucceeds() {
        doReturn(true).when(goblintServer).isAlive();

        when(gobPieConfiguration.getPreAnalyzeCommand()).thenReturn(new String[]{});
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);
        when(gobPieConfiguration.getGoblintConf()).thenReturn("goblint.json");

        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult()));
        when(goblintService.reset_config()).thenReturn(CompletableFuture.completedFuture(null));
        when(goblintService.read_config(new Params(new File("goblint.json").getAbsolutePath()))).thenReturn(CompletableFuture.completedFuture(null));

        GoblintConfWatcher goblintConfWatcher = new GoblintConfWatcher(magpieServer, goblintService, gobPieConfiguration, new FileWatcher(Path.of("")));
        GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfiguration, goblintConfWatcher);
        goblintAnalysis.analyze(files, analysisConsumer, true);

        assertTrue(goblintConfWatcher.configValid);

        // Check that if config is valid and not modified, refresh is not necessary and is skipped
        goblintAnalysis.analyze(files, analysisConsumer, true);
        assertTrue(goblintConfWatcher.configValid);
    }

    @Test
    void refreshGoblintConfigFails() {
        doReturn(true).when(goblintServer).isAlive();

        when(gobPieConfiguration.getPreAnalyzeCommand()).thenReturn(new String[]{});
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);
        when(gobPieConfiguration.getGoblintConf()).thenReturn("goblint.json");

        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult()));
        when(goblintService.reset_config()).thenReturn(CompletableFuture.completedFuture(null));
        when(goblintService.read_config(new Params())).thenReturn(CompletableFuture.completedFuture(null));

        FileWatcher fileWatcher = spy(new FileWatcher(Path.of("")));
        when(fileWatcher.checkModified()).thenReturn(true);

        GoblintConfWatcher goblintConfWatcher = new GoblintConfWatcher(magpieServer, goblintService, gobPieConfiguration, fileWatcher);
        GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfiguration, goblintConfWatcher);
        goblintAnalysis.analyze(files, analysisConsumer, true);

        assertFalse(goblintConfWatcher.configValid);
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Goblint was unable to successfully read the new configuration: ")));
    }

}
