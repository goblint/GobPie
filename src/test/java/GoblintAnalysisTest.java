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
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
class GoblintAnalysisTest extends TestHelper {

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

    @BeforeEach
    void before() {
        mockGoblintServerIsAlive(goblintServer, goblintConfWatcher);
        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.incrementalAnalysis()).thenReturn(true);
        // Mock that Goblint returns some messages (if applicable)
        when(goblintService.messages()).thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
    }

    /**
     * Mock test to ensure @analyze function
     * messages user when analysis fails due to future throwing an exception
     */
    @Test
    void analyzeFailedWithFailedFuture() {
        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.failedFuture(new Throwable(" Testing failed analysis")));

        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that failed analysis has been logged
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("---------------------- Analysis started ----------------------")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("--------------------- Analysis failed  ----------------------")));

        // Verify that user is notified about the failed analysis
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie started analyzing the code."));
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Error, "GobPie failed to analyze the code:\n Testing failed analysis"));
    }

    /**
     * Mock test to ensure @analyze function
     * messages user when analysis fails due to Goblint aborting
     */
    @Test
    void analyzeFailedWithGoblintAborted() {
        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult(List.of("Aborted"))));

        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that failed analysis has been logged
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("---------------------- Analysis started ----------------------")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("--------------------- Analysis failed  ----------------------")));

        // Verify that user is notified about the failed analysis
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie started analyzing the code."));
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Error, "GobPie failed to analyze the code:\nThe running analysis has been aborted."));
    }

    /**
     * Mock test to ensure @analyze function
     * messages user when analysis fails due to Goblint responding with VerifyError
     */
    @Test
    void analyzeFailedWithGoblintVerifyError() {
        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult(List.of("VerifyError"))));

        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that failed analysis has been logged
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("---------------------- Analysis started ----------------------")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("--------------------- Analysis failed  ----------------------")));

        // Verify that user is notified about the failed analysis
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie started analyzing the code."));
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Error, "GobPie failed to analyze the code:\nAnalysis returned VerifyError."));
    }

    /**
     * Mock test to ensure @analyze function
     * behaviour in abort situation
     */
    @Test
    void abortAnalysis() throws IOException {
        // Mock that the analyses of Goblint have started but not completed (still run)
        CompletableFuture<GoblintAnalysisResult> runningProcess1 = new CompletableFuture<>();
        CompletableFuture<GoblintAnalysisResult> runningProcess2 = new CompletableFuture<>();

        // Mock that goblintServer.abortAnalysis completes normally when called
        doNothing().when(goblintServer).abortAnalysis();

        // Call analyze method twice
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(runningProcess1);
        goblintAnalysis.analyze(files, analysisConsumer, true);
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("---------------------- Analysis started ----------------------")));
        systemOut.clear();

        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(runningProcess2);
        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that aborted analysis has been properly logged and new one started
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("--------------- This analysis has been aborted -------------")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("---------------------- Analysis started ----------------------")));

        // Verify that the user has been notified about starting an analysis twice and about finishing once
        verify(magpieServer, times(2)).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie started analyzing the code."));
        verify(magpieServer, times(0)).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie finished analyzing the code."));

        // Verify that abortAnalysis was indeed called once
        verify(goblintServer).abortAnalysis();

        // Finish the running analysis properly
        systemOut.clear();
        runningProcess2.complete(new GoblintAnalysisResult(List.of("Success")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("--------------------- Analysis finished ----------------------")));
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie finished analyzing the code."));
    }

    /**
     * Mock test to ensure @analyze function
     * behaves correctly when abort fails
     */
    @Test
    void abortAnalysisFails() throws IOException {
        // Mock that the analyses of Goblint have started but not completed (still run)
        CompletableFuture<GoblintAnalysisResult> runningProcess = new CompletableFuture<>();
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(runningProcess);

        // Mock that goblintServer.abortAnalysis throws an exception when called
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
        // A process that must be run before analysis
        String processPrintout = "'Hello'";
        List<String> preAnalyzeCommand = List.of("echo", processPrintout);
        when(gobPieConfiguration.preAnalyzeCommand()).thenReturn(preAnalyzeCommand);

        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult(List.of("Success"))));

        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that preAnalysis was indeed called once
        verify(goblintServer).preAnalyse();
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("PreAnalysis command ran: '" + preAnalyzeCommand + "'")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("PreAnalysis command finished.")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains(processPrintout)));
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie started analyzing the code."));
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie finished analyzing the code."));
    }

    /**
     * Mock test to ensure @preAnalyse function
     * is functional when preAnalyzeCommand is empty
     */
    @Test
    void preAnalyseEmpty() {
        // Mock that the command to execute is empty
        List<String> preAnalyzeCommand = new ArrayList<>();
        when(gobPieConfiguration.preAnalyzeCommand()).thenReturn(preAnalyzeCommand);

        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult(List.of("Success"))));

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
        // Mock that the command to execute is null
        when(gobPieConfiguration.preAnalyzeCommand()).thenReturn(null);

        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult(List.of("Success"))));

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
        // Mock that the command to execute is not empty and is something that is not a valid command
        List<String> preAnalyzeCommand = List.of("asdf");
        when(gobPieConfiguration.preAnalyzeCommand()).thenReturn(preAnalyzeCommand);

        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult(List.of("Success"))));

        goblintAnalysis.analyze(files, analysisConsumer, true);

        // Verify that preAnalysis was indeed called once
        verify(goblintServer).preAnalyse();
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie started analyzing the code."));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("PreAnalysis command ran: '" + preAnalyzeCommand + "'")));
        assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Running preAnalysis command failed. ")));
        //verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Warning, "Running preAnalysis command failed. ")); // TODO?
        verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Info, "GobPie finished analyzing the code."));
    }

}