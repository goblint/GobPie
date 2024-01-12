import analysis.GoblintAnalysis;
import api.GoblintService;
import api.messages.params.AnalyzeParams;
import com.ibm.wala.classLoader.Module;
import goblintserver.GoblintConfWatcher;
import goblintserver.GoblintServer;
import gobpie.GobPieConfiguration;
import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.MagpieServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

class GoblintAnalysisTest {

    @Test
    void abortAnalysis() throws IOException {

        // Mock everything needed for creating GoblintAnalysis
        MagpieServer magpieServer = mock(MagpieServer.class);
        GoblintServer goblintServer = mock(GoblintServer.class);
        GoblintService goblintService = mock(GoblintService.class);
        GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
        GoblintConfWatcher goblintConfWatcher = mock(GoblintConfWatcher.class);

        GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfiguration, goblintConfWatcher);

        // Mock that GoblintServer is alive and everything is fine with Goblint's configuration file
        when(goblintServer.isAlive()).thenReturn(true);
        when(goblintConfWatcher.refreshGoblintConfig()).thenReturn(true);

        // Mock that the analyses of goblint have started but not completed (still run)
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(new CompletableFuture<>());

        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);

        // Mock the arguments for calling the goblintAnalyze.analyze method
        // And call the method twice
        Collection<? extends Module> files = new ArrayDeque<>();
        AnalysisConsumer analysisConsumer = mock(AnalysisConsumer.class);
        goblintAnalysis.analyze(files, analysisConsumer, true);
        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that abortAnalysis was indeed called once
        verify(goblintServer).abortAnalysis();
    }

}