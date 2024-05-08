import analysis.GoblintAnalysis;
import analysis.GoblintCFGAnalysisResult;
import analysis.GoblintMessagesAnalysisResult;
import api.GoblintService;
import api.json.GoblintMessageJsonHandler;
import api.messages.GoblintAnalysisResult;
import api.messages.GoblintFunctionsResult;
import api.messages.GoblintMessagesResult;
import api.messages.GoblintPosition;
import api.messages.params.AnalyzeParams;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.util.collections.Pair;
import goblintserver.GoblintConfWatcher;
import goblintserver.GoblintServer;
import gobpie.GobPieConfiguration;
import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.MagpieServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

public class GoblintMessagesTest extends TestHelper {

    private Gson gson = new GoblintMessageJsonHandler(new HashMap<>()).getDefaultGsonBuilder().create();
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

    Type messagesTypeToken = new TypeToken<List<GoblintMessagesResult>>() {
    }.getType();

    Type functionsTypeToken = new TypeToken<List<GoblintFunctionsResult>>() {
    }.getType();

    @BeforeEach
    public void before() {
        mockGoblintServerIsAlive(goblintServer, goblintConfWatcher);
        // Mock that the command to execute is empty
        when(gobPieConfiguration.getPreAnalyzeCommand()).thenReturn(new String[]{});
        // Mock that the analyses of Goblint have started and completed
        when(goblintService.analyze(new AnalyzeParams(false))).thenReturn(CompletableFuture.completedFuture(new GoblintAnalysisResult()));
        // Mock that the incremental analysis is turned off (TODO: not sure why this is checked in reanalyze?)
        when(gobPieConfiguration.useIncrementalAnalysis()).thenReturn(true);
    }

    private <T> List<T> readGoblintResponseJson(String resource, Type typeToken) throws IOException {
        String messages = Files.readString(
                Path.of(Objects.requireNonNull(GoblintMessagesTest.class.getResource(resource)).getPath())
        );
        return gson.fromJson(messages, typeToken);
    }


    /**
     * Mock test to ensure that the Goblint warnings received from a response in JSON format
     * are correctly converted to {@link AnalysisResult} objects
     * and passed to {@link MagpieServer} via {@link AnalysisConsumer}.
     *
     * @throws IOException when reading messagesResponse.json from resources fails.
     */
    @Test
    public void testConvertMessagesFromJson() throws IOException {
        List<GoblintMessagesResult> goblintMessagesResults = readGoblintResponseJson("messagesResponse.json", messagesTypeToken);
        when(goblintService.messages()).thenReturn(CompletableFuture.completedFuture(goblintMessagesResults));
        when(gobPieConfiguration.showCfg()).thenReturn(false);
        goblintAnalysis.analyze(files, analysisConsumer, true);

        URL emptyUrl = new File("").toURI().toURL();
        GoblintPosition defaultPos = new GoblintPosition(1, 1, 1, 1, emptyUrl);
        URL exampleUrl = new File("src/example.c").toURI().toURL();
        List<AnalysisResult> response = new ArrayList<>();
        response.add(
                new GoblintMessagesAnalysisResult(
                        defaultPos,
                        "[Deadcode] Logical lines of code (LLoC) summary",
                        "Info",
                        List.of(
                                Pair.make(defaultPos, "live: 12"),
                                Pair.make(defaultPos, "dead: 0"),
                                Pair.make(defaultPos, "total lines: 12")
                        )
                )
        );
        response.add(
                new GoblintMessagesAnalysisResult(
                        new GoblintPosition(4, 4, 4, 12, exampleUrl),
                        "[Race] Memory location myglobal (race with conf. 110)",
                        "Warning",
                        List.of(
                                Pair.make(
                                        new GoblintPosition(10, 10, 2, 21, exampleUrl),
                                        "write with [mhp:{tid=[main, t_fun@src/example.c:17:3-17:40#top]}, lock:{mutex1}, thread:[main, t_fun@src/example.c:17:3-17:40#top]] (conf. 110)  (exp: & myglobal)"
                                ),
                                Pair.make(
                                        new GoblintPosition(19, 19, 2, 21, exampleUrl),
                                        "write with [mhp:{tid=[main]; created={[main, t_fun@src/example.c:17:3-17:40#top]}}, lock:{mutex2}, thread:[main]] (conf. 110)  (exp: & myglobal)"
                                )
                        )
                )
        );
        response.add(
                new GoblintMessagesAnalysisResult(
                        defaultPos,
                        "[Race] Memory locations race summary",
                        "Info",
                        List.of(
                                Pair.make(defaultPos, "safe: 0"),
                                Pair.make(defaultPos, "vulnerable: 0"),
                                Pair.make(defaultPos, "unsafe: 1"),
                                Pair.make(defaultPos, "total memory locations: 1")
                        )
                )
        );
        verify(analysisConsumer).consume(response, "GobPie");
    }

    /**
     * Mock test to ensure that the Goblint warnings with Explode received from a response in JSON format
     * are correctly converted to {@link AnalysisResult} objects
     * and passed to {@link MagpieServer} via {@link AnalysisConsumer}.
     *
     * @throws IOException when reading messagesResponse.json from resources fails.
     */
    @Test
    public void testConvertMessagesFromJsonWithExplode() throws IOException {
        List<GoblintMessagesResult> goblintMessagesResults = readGoblintResponseJson("messagesResponse.json", messagesTypeToken);
        when(goblintService.messages()).thenReturn(CompletableFuture.completedFuture(goblintMessagesResults));
        when(gobPieConfiguration.showCfg()).thenReturn(false);
        when(gobPieConfiguration.explodeGroupWarnings()).thenReturn(true);
        goblintAnalysis.analyze(files, analysisConsumer, true);

        URL emptyUrl = new File("").toURI().toURL();
        GoblintPosition defaultPos = new GoblintPosition(1, 1, 1, emptyUrl);
        URL exampleUrl = new File("src/example.c").toURI().toURL();
        List<AnalysisResult> response = new ArrayList<>();
        response.add(
                new GoblintMessagesAnalysisResult(
                        defaultPos,
                        "[Deadcode] Logical lines of code (LLoC) summary",
                        "Info",
                        List.of(
                                Pair.make(defaultPos, "live: 12"),
                                Pair.make(defaultPos, "dead: 0"),
                                Pair.make(defaultPos, "total lines: 12")
                        )
                )
        );
        response.add(
                new GoblintMessagesAnalysisResult(
                        new GoblintPosition(10, 10, 2, 21, exampleUrl),
                        "[Race] Group: Memory location myglobal (race with conf. 110)\n" +
                                "write with [mhp:{tid=[main, t_fun@src/example.c:17:3-17:40#top]}, lock:{mutex1}, thread:[main, t_fun@src/example.c:17:3-17:40#top]] (conf. 110)  (exp: & myglobal)",
                        "Warning",
                        List.of(Pair.make(
                                        new GoblintPosition(19, 19, 2, 21, exampleUrl),
                                        "write with [mhp:{tid=[main]; created={[main, t_fun@src/example.c:17:3-17:40#top]}}, lock:{mutex2}, thread:[main]] (conf. 110)  (exp: & myglobal)"
                                )
                        )
                )

        );
        response.add(
                new GoblintMessagesAnalysisResult(
                        new GoblintPosition(19, 19, 2, 21, exampleUrl),
                        "[Race] Group: Memory location myglobal (race with conf. 110)\n" +
                                "write with [mhp:{tid=[main]; created={[main, t_fun@src/example.c:17:3-17:40#top]}}, lock:{mutex2}, thread:[main]] (conf. 110)  (exp: & myglobal)",
                        "Warning",
                        List.of(Pair.make(
                                        new GoblintPosition(10, 10, 2, 21, exampleUrl),
                                        "write with [mhp:{tid=[main, t_fun@src/example.c:17:3-17:40#top]}, lock:{mutex1}, thread:[main, t_fun@src/example.c:17:3-17:40#top]] (conf. 110)  (exp: & myglobal)"
                                )
                        )
                )
        );

        response.add(
                new GoblintMessagesAnalysisResult(
                        defaultPos,
                        "[Race] Memory locations race summary",
                        "Info",
                        List.of(
                                Pair.make(defaultPos, "safe: 0"),
                                Pair.make(defaultPos, "vulnerable: 0"),
                                Pair.make(defaultPos, "unsafe: 1"),
                                Pair.make(defaultPos, "total memory locations: 1")
                        )
                )
        );
        verify(analysisConsumer).consume(response, "GobPie");

    }

    /**
     * Mock test to ensure that the Goblint warnings received from a response in JSON format
     * are correctly converted to {@link AnalysisResult} objects
     * and passed to {@link MagpieServer} via {@link AnalysisConsumer}.
     *
     * @throws IOException when reading messagesResponsePiece.json from resources fails.
     */
    @Test
    public void testConvertMessagesPieceFromJson() throws IOException {
        List<GoblintMessagesResult> goblintMessagesResults = readGoblintResponseJson("messagesResponsePiece.json", messagesTypeToken);
        when(goblintService.messages()).thenReturn(CompletableFuture.completedFuture(goblintMessagesResults));
        when(gobPieConfiguration.showCfg()).thenReturn(false);
        goblintAnalysis.analyze(files, analysisConsumer, true);

        URL assertUrl = new File("src/01-assert.c").toURI().toURL();
        URL npUrl = new File("src/10-nullpointer-dereference-simple.c").toURI().toURL();
        List<AnalysisResult> response = new ArrayList<>();
        response.add(
                new GoblintMessagesAnalysisResult(
                        new GoblintPosition(10, 10, 2, 26, assertUrl),
                        "[Assert] Assertion \"success\" will succeed",
                        "Success"
                )
        );
        response.add(
                new GoblintMessagesAnalysisResult(
                        new GoblintPosition(11, 11, 2, 23, assertUrl),
                        "[Assert] Assertion \"fail\" will fail.",
                        "Error"
                )
        );
        response.add(
                new GoblintMessagesAnalysisResult(
                        new GoblintPosition(12, 12, 2, 31, assertUrl),
                        "[Assert] Assertion \"unknown == 4\" is unknown.",
                        "Warning"
                )
        );
        response.add(
                new GoblintMessagesAnalysisResult(
                        new GoblintPosition(7, 7, 8, 14, npUrl),
                        "[Behavior > Undefined > NullPointerDereference][CWE-476] May dereference NULL pointer in context 13",
                        "Warning"
                )
        );
        response.add(
                new GoblintMessagesAnalysisResult(
                        new GoblintPosition(1, 1, 1, new File("").toURI().toURL()),
                        "[Deadcode] Function 'main' does not return",
                        "Warning"
                )
        );
        verify(analysisConsumer).consume(response, "GobPie");
    }

    /**
     * Mock test to ensure that the Goblint functions received from a response in JSON format
     * are correctly converted to {@link GoblintCFGAnalysisResult} objects
     * and passed to {@link MagpieServer} via {@link AnalysisConsumer}.
     *
     * @throws IOException when reading messagesResponse.json from resources fails.
     */
    @Test
    public void testConvertFunctionsFromJson() throws IOException {
        List<GoblintFunctionsResult> goblintFunctionsResults = readGoblintResponseJson("functionsResponse.json", functionsTypeToken);
        when(goblintService.functions()).thenReturn(CompletableFuture.completedFuture(goblintFunctionsResults));
        when(gobPieConfiguration.showCfg()).thenReturn(true);
        when(goblintService.messages()).thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
        goblintAnalysis.analyze(files, analysisConsumer, true);

        URL emptyUrl = new File("").toURI().toURL();
        GoblintPosition defaultPos = new GoblintPosition(1, 1, 1, 1, emptyUrl);
        URL exampleUrl = new File("src/example.c").toURI().toURL();
        List<AnalysisResult> response = new ArrayList<>();
        response.add(
                new GoblintCFGAnalysisResult(
                        new GoblintPosition(8, 13, 0, 0, exampleUrl),
                        "show cfg",
                        "t_fun"
                )
        );
        response.add(
                new GoblintCFGAnalysisResult(
                        new GoblintPosition(15, 23, 0, 0, exampleUrl),
                        "show arg",
                        "<arg>"
                )
        );
        response.add(
                new GoblintCFGAnalysisResult(
                        new GoblintPosition(15, 23, 0, 0, exampleUrl),
                        "show cfg",
                        "main"
                )
        );
        verify(analysisConsumer).consume(response, "GobPie");
    }

}