import analysis.GoblintAnalysis;
import api.GoblintService;
import api.messages.params.AnalyzeParams;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.ibm.wala.classLoader.Module;
import goblintserver.GoblintConfWatcher;
import goblintserver.GoblintServer;
import gobpie.GobPieConfiguration;
import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.MagpieServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.stream.SystemOut;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
class GoblintAnalysisTest {

    @SystemStub
    private SystemOut systemOut;

    /**
     * Mock test to ensure @analyze function
     * behaviour in abort situation
     */
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

        // Mock that the analyses of Goblint have started but not completed (still run)
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
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("--------------- This analysis has been aborted -------------")));
    }

    /**
     * Mock test to ensure @preAnalyse function
     * is functional and is called out in @analyze function
     */


    @Test
    void preAnalysetest() {
        // Mock everything needed for creating preAnalysis
        MagpieServer magpieServer = mock(MagpieServer.class);
        GoblintServer goblintServer = mock(GoblintServer.class);
        GoblintService goblintService = mock(GoblintService.class);
        GobPieConfiguration gobPieConfigurationMock = mock(GobPieConfiguration.class);
        GoblintConfWatcher goblintConfWatcher = mock(GoblintConfWatcher.class);


        GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfigurationMock, goblintConfWatcher);

        // Mock that GoblintServer is alive and everything is fine with Goblint's configuration file
        when(goblintServer.isAlive()).thenReturn(true);
        when(goblintConfWatcher.refreshGoblintConfig()).thenReturn(true);

        // Mock that the command to execute is not empty
        String[] preAnalyzeCommand = new String[]{"cmake", "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON", "-B", "build"};
        when(gobPieConfigurationMock.getPreAnalyzeCommand()).thenReturn(new String[]{"cmake", "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON", "-B", "build"});

        // Mock that the analyses of goblint have started but not completed (still run)
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(new CompletableFuture<>());

        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfigurationMock.useIncrementalAnalysis()).thenReturn(true);

        // Mock the arguments for calling the goblintAnalyze.analyze method
        Collection<? extends Module> files = new ArrayDeque<>();
        AnalysisConsumer analysisConsumer = mock(AnalysisConsumer.class);
        goblintAnalysis.analyze(files, analysisConsumer, true);


        // Verify that preAnalysis was indeed called once
        verify(goblintServer).preAnalyse();
    }

    /**
     * * Mock test to ensure @preAnalyse function
     * <p>
     * <p>
     * is functional and is called out in @analyze function
     */
    @Test
    void preanalyzeError() {
        // Mock everything needed for creating preAnalysis
        MagpieServer magpieServer = mock(MagpieServer.class);
        GoblintServer goblintServer = mock(GoblintServer.class);
        GoblintService goblintService = mock(GoblintService.class);
        GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
        GoblintConfWatcher goblintConfWatcher = mock(GoblintConfWatcher.class);
        LoggerContext context = new LoggerContext();
        Logger logger = context.getLogger("testLogger");


        GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfiguration, goblintConfWatcher);

        // Mock that GoblintServer is alive and everything is fine with Goblint's configuration file
        when(goblintServer.isAlive()).thenReturn(true);
        when(goblintConfWatcher.refreshGoblintConfig()).thenReturn(true);

        // Mock that the command to execute is not empty
        // Make mistake to catch exception -  correct - {"cmake", "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON", "-B", "build"}
        String[] preAnalyzeCommand = new String[]{"cmake", "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON", "-V", "bdghdhgfjhy"};
        when(gobPieConfiguration.getPreAnalyzeCommand()).thenReturn(preAnalyzeCommand);

        // Mock that the analyses of goblint have started but not completed (still run)
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(new CompletableFuture<>());

        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);

        // Mock the arguments for calling the goblintAnalyze.analyze method
        Collection<? extends Module> files = new ArrayDeque<>();
        AnalysisConsumer analysisConsumer = mock(AnalysisConsumer.class);
        goblintAnalysis.analyze(files, analysisConsumer, true);

        goblintServer.preAnalyse();

        System.out.println(logger.getLoggerContext().toString());

        assert (logger.getLoggerContext().toString().contains("Running preanalysis command failed. "));

        //verify(log, times (1)).info("Running preanalysis command failed. ");

        // Verify that preAnalysis was indeed called once
        verify(goblintServer).preAnalyse();
    }


}