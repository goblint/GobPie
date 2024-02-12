import analysis.GoblintAnalysis;
import api.GoblintService;
import api.messages.GoblintAnalysisResult;
import api.messages.params.AnalyzeParams;
import com.ibm.wala.classLoader.Module;
import goblintserver.GoblintConfWatcher;
import goblintserver.GoblintServer;
import gobpie.GobPieConfiguration;
import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.MagpieServer;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.stream.SystemOut;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
class GoblintAnalysisTest {

    @SystemStub
    private SystemOut systemOut;

    @Test
    void analyzeFailed() {
        // Mock everything needed for creating GoblintAnalysis
        MagpieServer magpieServer = mock(MagpieServer.class);
        GoblintServer goblintServer = mock(GoblintServer.class);
        GoblintService goblintService = mock(GoblintService.class);
        GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
        GoblintConfWatcher goblintConfWatcher = mock(GoblintConfWatcher.class);
        GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfiguration, goblintConfWatcher);

        // Mock that GoblintServer is alive and everything is fine with Goblint's configuration file
        doReturn(true).when(goblintServer).isAlive();
        when(goblintConfWatcher.refreshGoblintConfig()).thenReturn(true);

        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.failedFuture(new Throwable(" Testing failed analysis")));

        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);

        // Mock the arguments for calling the goblintAnalyze.analyze method
        // And call the method twice
        Collection<? extends Module> files = new ArrayDeque<>();
        AnalysisConsumer analysisConsumer = mock(AnalysisConsumer.class);
        goblintAnalysis.analyze(files, null, true);

        // Verify that Analysis has failed
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("---------------------- Analysis started ----------------------")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("--------------------- Analysis failed  ----------------------")));

        // Verify that user is notified about the failed analysis
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie started analyzing the code."));
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Error, "GobPie failed to analyze the code:\n Testing failed analysis"));
    }


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
        doReturn(true).when(goblintServer).isAlive();
        when(goblintConfWatcher.refreshGoblintConfig()).thenReturn(true);

        // Mock that the analyses of Goblint have started but not completed (still run)
        CompletableFuture<GoblintAnalysisResult> runningProcess = new CompletableFuture<>();
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(runningProcess);

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
        runningProcess.complete(null);
    }

    /**
     * Mock test to ensure @preAnalyse function
     * is functional and is called out in @analyze function
     */

    @Test
    void preAnalyseTest() {
        // Mock everything needed for creating preAnalysis
        MagpieServer magpieServer = mock(MagpieServer.class);
        GoblintService goblintService = mock(GoblintService.class);
        GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
        GoblintServer goblintServer = spy(new GoblintServer(magpieServer, gobPieConfiguration));
        GoblintConfWatcher goblintConfWatcher = mock(GoblintConfWatcher.class);
        GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfiguration, goblintConfWatcher);

        // Mock that GoblintServer is alive and everything is fine with Goblint's configuration file
        doReturn(true).when(goblintServer).isAlive();
        when(goblintConfWatcher.refreshGoblintConfig()).thenReturn(true);

        // Mock that the command to execute is not empty
        String[] preAnalyzeCommand = new String[]{"cmake", "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON", "-B", "build"};
        when(gobPieConfiguration.getPreAnalyzeCommand()).thenReturn(preAnalyzeCommand);

        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(null));

        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);

        // Mock the arguments for calling the goblintAnalyze.analyze method
        Collection<? extends Module> files = new ArrayDeque<>();
        AnalysisConsumer analysisConsumer = mock(AnalysisConsumer.class);
        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that preAnalysis was indeed called once
        verify(goblintServer).preAnalyse();
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Preanalyze command ran: ")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Preanalyze command finished.")));
    }

    /**
     * * Mock test to ensure @preAnalyse function
     * is functional and is called out in @analyze function
     */
    @Test
    void preAnalyseEmpty() {
        // Mock everything needed for creating preAnalysis
        MagpieServer magpieServer = mock(MagpieServer.class);
        GoblintService goblintService = mock(GoblintService.class);
        GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
        GoblintServer goblintServer = spy(new GoblintServer(magpieServer, gobPieConfiguration));
        GoblintConfWatcher goblintConfWatcher = mock(GoblintConfWatcher.class);
        GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfiguration, goblintConfWatcher);

        // Mock that GoblintServer is alive and everything is fine with Goblint's configuration file
        doReturn(true).when(goblintServer).isAlive();
        when(goblintConfWatcher.refreshGoblintConfig()).thenReturn(true);

        // Mock that Goblint returns some messages
        when(goblintService.messages()).thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));

        // Mock that the command to execute is empty
        String[] preAnalyzeCommand = new String[]{};
        when(gobPieConfiguration.getPreAnalyzeCommand()).thenReturn(preAnalyzeCommand);

        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult()));

        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);

        // Mock the arguments for calling the goblintAnalyze.analyze method
        Collection<? extends Module> files = new ArrayDeque<>();
        AnalysisConsumer analysisConsumer = mock(AnalysisConsumer.class);
        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that preAnalysis was indeed called once
        verify(goblintServer).preAnalyse();
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie started analyzing the code."));
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie finished analyzing the code."));
    }

    @Test
    void preAnalyseNull() {
        // Mock everything needed for creating preAnalysis
        MagpieServer magpieServer = mock(MagpieServer.class);
        GoblintService goblintService = mock(GoblintService.class);
        GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
        GoblintServer goblintServer = spy(new GoblintServer(magpieServer, gobPieConfiguration));
        GoblintConfWatcher goblintConfWatcher = mock(GoblintConfWatcher.class);
        GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfiguration, goblintConfWatcher);

        // Mock that GoblintServer is alive and everything is fine with Goblint's configuration file
        doReturn(true).when(goblintServer).isAlive();
        when(goblintConfWatcher.refreshGoblintConfig()).thenReturn(true);

        // Mock that Goblint returns some messages
        when(goblintService.messages()).thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));

        // Mock that the command to execute is null
        when(gobPieConfiguration.getPreAnalyzeCommand()).thenReturn(null);

        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult()));

        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);

        // Mock the arguments for calling the goblintAnalyze.analyze method
        Collection<? extends Module> files = new ArrayDeque<>();
        AnalysisConsumer analysisConsumer = mock(AnalysisConsumer.class);
        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that preAnalysis was indeed called once
        verify(goblintServer).preAnalyse();
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie started analyzing the code."));
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie finished analyzing the code."));
    }

    @Test
    void preAnalyseError() {
        // Mock everything needed for creating preAnalysis
        MagpieServer magpieServer = mock(MagpieServer.class);
        GoblintService goblintService = mock(GoblintService.class);
        GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
        GoblintServer goblintServer = spy(new GoblintServer(magpieServer, gobPieConfiguration));
        GoblintConfWatcher goblintConfWatcher = mock(GoblintConfWatcher.class);
        GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfiguration, goblintConfWatcher);

        // Mock that GoblintServer is alive and everything is fine with Goblint's configuration file
        doReturn(true).when(goblintServer).isAlive();
        when(goblintConfWatcher.refreshGoblintConfig()).thenReturn(true);

        // Mock that the command to execute is not empty
        // Make mistake to catch exception -  correct - {"cmake", "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON", "-B", "build"}
        String[] preAnalyzeCommand = new String[]{"cmake", "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON", "-B", "build"};
        when(gobPieConfiguration.getPreAnalyzeCommand()).thenReturn(preAnalyzeCommand);

        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(null));
        //.throw

        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);

        // Mock the arguments for calling the goblintAnalyze.analyze method
        Collection<? extends Module> files = new ArrayDeque<>();
        AnalysisConsumer analysisConsumer = mock(AnalysisConsumer.class);
        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that preAnalysis was indeed called once
        verify(goblintServer).preAnalyse();
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie started analyzing the code."));
        //verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Warning, "Running preanalysis command failed."));
    }

}