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
import org.mockito.Mock;
import org.mockito.Spy;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.stream.SystemOut;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
class GoblintAnalysisTest {

    @Mock
    MagpieServer magpieServer = mock(MagpieServer.class);
    @Mock
    GoblintService goblintService = mock(GoblintService.class);
    @Mock
    GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
    @Spy
    GoblintServer goblintServer = spy(new GoblintServer(magpieServer, gobPieConfiguration));
    @Mock
    GoblintConfWatcher goblintConfWatcher = mock(GoblintConfWatcher.class);
    GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfiguration, goblintConfWatcher);
    // Mock the arguments (files and analysisConsumer) for calling the GoblintAnalyze.analyze method
    Collection<? extends Module> files = new ArrayDeque<>();
    AnalysisConsumer analysisConsumer = mock(AnalysisConsumer.class);
    @SystemStub
    private SystemOut systemOut;

    /**
     * A function to mock that GoblintServer is alive
     * and Goblint's configuration file is ok.
     */
    private void mockGoblintServerIsAlive(GoblintServer goblintServer) {
        doReturn(true).when(goblintServer).isAlive();
        when(goblintConfWatcher.refreshGoblintConfig()).thenReturn(true);
    }

    /**
     * Mock test to ensure @analyze function
     * messages user when analyzes fails
     */
    @Test
    void analyzeFailed() {
        mockGoblintServerIsAlive(goblintServer);

        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.failedFuture(new Throwable(" Testing failed analysis")));

        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);

        goblintAnalysis.analyze(files, analysisConsumer, true);

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
        // Mock server and change goblintAnalysis value
        GoblintServer goblintServer = mock(GoblintServer.class);
        GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfiguration, goblintConfWatcher);

        mockGoblintServerIsAlive(goblintServer);

        // Mock that the analyses of Goblint have started but not completed (still run)
        CompletableFuture<GoblintAnalysisResult> runningProcess = new CompletableFuture<>();
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(runningProcess);

        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);

        // Call analyze method twice
        goblintAnalysis.analyze(files, analysisConsumer, true);
        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that abortAnalysis was indeed called once
        verify(goblintServer).abortAnalysis();
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("--------------- This analysis has been aborted -------------")));
        runningProcess.complete(null);
    }

    /**
     * Mock test to ensure @analyze function
     * behaves correctly when abort fails
     */
    @Test
    void abortAnalysisFails() throws IOException {
        // Mock server and change goblintAnalysis value
        GoblintServer goblintServer = mock(GoblintServer.class);
        GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfiguration, goblintConfWatcher);

        mockGoblintServerIsAlive(goblintServer);

        // Mock that the analyses of Goblint have started but not completed (still run)
        CompletableFuture<GoblintAnalysisResult> runningProcess = new CompletableFuture<>();
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(runningProcess);

        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);

        // Mock that abortAnalysis throws an exception when called
        doThrow(new IOException()).when(goblintServer).abortAnalysis();

        // Call analyze method twice
        goblintAnalysis.analyze(files, analysisConsumer, true);
        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that abortAnalysis was indeed called once
        verify(goblintServer).abortAnalysis();
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Aborting analysis failed.")));
        runningProcess.complete(null);
    }

    /**
     * Mock test to ensure @preAnalyse function
     * is functional and is called out in @analyze function
     */
    @Test
    void preAnalyseTest() {
        mockGoblintServerIsAlive(goblintServer);

        // A process that must be run before analysis
        String processPrintout = "'Hello'";
        String[] preAnalyzeCommand = new String[]{"echo", processPrintout};
        when(gobPieConfiguration.getPreAnalyzeCommand()).thenReturn(preAnalyzeCommand);

        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(null));

        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);

        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that preAnalysis was indeed called once
        verify(goblintServer).preAnalyse();
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("PreAnalysis command ran: '" + Arrays.toString(preAnalyzeCommand) + "'")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("PreAnalysis command finished.")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains(processPrintout)));
    }

    /**
     * Mock test to ensure @preAnalyse function
     * is functional when preAnalyzeCommand is empty
     */
    @Test
    void preAnalyseEmpty() {
        mockGoblintServerIsAlive(goblintServer);

        // Mock that Goblint returns some messages
        when(goblintService.messages()).thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));

        // Mock that the command to execute is empty
        String[] preAnalyzeCommand = new String[]{};
        when(gobPieConfiguration.getPreAnalyzeCommand()).thenReturn(preAnalyzeCommand);

        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult()));

        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);

        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that preAnalysis was indeed called once
        verify(goblintServer).preAnalyse();
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie started analyzing the code."));
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie finished analyzing the code."));
    }

    /**
     * Mock test to ensure @preAnalyse function
     * is functional when preAnalyzeCommand is null
     */
    @Test
    void preAnalyseNull() {
        mockGoblintServerIsAlive(goblintServer);

        // Mock that Goblint returns some messages
        when(goblintService.messages()).thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));

        // Mock that the command to execute is null
        when(gobPieConfiguration.getPreAnalyzeCommand()).thenReturn(null);

        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult()));

        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);

        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that preAnalysis was indeed called once
        verify(goblintServer).preAnalyse();
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie started analyzing the code."));
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie finished analyzing the code."));
    }

    /**
     * Mock test to ensure @preAnalyse function
     * messages user when preAnalysis command fails
     */
    @Test
    void preAnalyseError() {
        mockGoblintServerIsAlive(goblintServer);

        // Mock that Goblint returns some messages
        when(goblintService.messages()).thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));

        // Mock that the command to execute is not empty and is something that is not a valid command
        String[] preAnalyzeCommand = new String[]{"asdf"};
        when(gobPieConfiguration.getPreAnalyzeCommand()).thenReturn(preAnalyzeCommand);

        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult()));

        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);

        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that preAnalysis was indeed called once
        String preAnalyzeCommandString = Arrays.toString(preAnalyzeCommand);
        verify(goblintServer).preAnalyse();
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie started analyzing the code."));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("PreAnalysis command ran: '" + preAnalyzeCommandString + "'")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Running preAnalysis command failed. ")));
        //verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Warning, "Running preAnalysis command failed. ")); // TODO?
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie finished analyzing the code."));
    }

}