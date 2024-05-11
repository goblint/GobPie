import com.google.gson.Gson;
import gobpie.GobPieConfReader;
import gobpie.GobPieConfiguration;
import gobpie.GobPieException;
import magpiebridge.core.MagpieServer;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.stream.SystemOut;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(SystemStubsExtension.class)
class GobPieConfTest {

    @TempDir
    private Path tempDir;
    @SystemStub
    private SystemOut systemOut;

    @Mock
    MagpieServer magpieServer = mock(MagpieServer.class);

    /*
     * A function that mocks MagpieServer, gets absolute
     * file path and returns new GobPieConfReader.
     */
    GobPieConfReader preFileSetup(Integer fileNumber) {
        // Mock everything needed for creating GobPieConfReader
        String fileName = "gobpieTest" + fileNumber + ".json";
        String gobPieConfFileName = Objects.requireNonNull(GobPieConfTest.class.getResource(fileName)).getFile();
        return new GobPieConfReader(magpieServer, gobPieConfFileName);
    }

    /**
     * Mock test to ensure @readGobPieConfiguration function
     * reads GobPie configuration
     */
    @Test
    void testReadGobPieConfiguration() {
        GobPieConfReader gobPieConfReader = preFileSetup(1);
        GobPieConfiguration expectedGobPieConfiguration =
                new GobPieConfiguration.Builder()
                        .setGoblintConf("goblint.json")
                        .setGoblintExecutable("/home/user/goblint/analyzer/goblint")
                        .setPreAnalyzeCommand(List.of("echo", "'hello'"))
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

    /**
     * Mock test to ensure @readGobPieConfiguration function
     * reads Complete GobPie configuration with different Boolean values
     */
    @Test
    void testReadCompleteGobPieConfiguration() {
        GobPieConfReader gobPieConfReader = preFileSetup(2);
        GobPieConfiguration expectedGobPieConfiguration =
                new GobPieConfiguration.Builder()
                        .setGoblintConf("goblint.json")
                        .setGoblintExecutable("/home/user/goblint/analyzer/goblint")
                        .setPreAnalyzeCommand(List.of("cmake", "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON", "-B", "build"))
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

    /**
     * Mock test to ensure @readGobPieConfiguration function
     * accurately retrieves default values
     */
    @Test
    void testGobPieConfigurationDefaultValues() {
        GobPieConfReader gobPieConfReader = preFileSetup(3);
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

    /**
     * Mock test to ensure @readGobPieConfiguration function
     * throws an exception when there is a syntax error in the JSON
     */
    @Test
    void testReadGobPieConfigurationWithWrongJSONSyntax() {
        GobPieConfReader gobPieConfReader = preFileSetup(4);
        GobPieException thrown = assertThrows(GobPieException.class, gobPieConfReader::readGobPieConfiguration);
        assertEquals("GobPie configuration file syntax is wrong.", thrown.getMessage());
    }

    /**
     * Mock test to ensure @readGobPieConfiguration function
     * checks for the presence of an unexpected field in the configuration
     */
    @Test
    void testReadGobPieConfigurationWithExtraField() {
        GobPieConfReader gobPieConfReader = preFileSetup(5);
        GobPieException thrown = assertThrows(GobPieException.class, gobPieConfReader::readGobPieConfiguration);
        assertEquals("There was an unknown option \"extraField\" in the GobPie configuration. Please check for any typos.", thrown.getMessage());
    }

    /**
     * Mock test to ensure @readGobPieConfiguration function
     * messages user when the configuration file is absent
     * in the root directory
     **/
    @Test
    void testGobPieConfigurationFileMissingInRoot() throws IOException, ExecutionException, InterruptedException {
        // Mock everything needed for creating GobPieConfReader
        MagpieServer magpieServer = mock(MagpieServer.class);
        Path tempGobpieConfFilePath = tempDir.resolve("gobpieTestTemp1.json");
        GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, tempGobpieConfFilePath.toString());

        assertFalse(Files.exists(tempGobpieConfFilePath));

        CompletableFuture<GobPieConfiguration> future = CompletableFuture.supplyAsync(gobPieConfReader::readGobPieConfiguration);

        GobPieConfiguration expectedGobPieConfiguration =
                new GobPieConfiguration.Builder()
                        .setGoblintConf("goblint.json")
                        .setGoblintExecutable("goblint")
                        .setAbstractDebugging(false)
                        .setShowCfg(false)
                        .setIncrementalAnalysis(true)
                        .setExplodeGroupWarnings(false)
                        .createGobPieConfiguration();

        Thread.sleep(10); // TODO: real sketchy hack

        // Write the expected conf into temporary file
        Gson gson = new Gson();
        String json = gson.toJson(expectedGobPieConfiguration);
        Files.write(tempGobpieConfFilePath, json.getBytes());

        // Assert that the file was indeed not present;
        // the user is notified;
        // and the configuration is successfully read when the configuration file is created
        try {
            GobPieConfiguration actualGobPieConfiguration = future.get(100, TimeUnit.MILLISECONDS);
            String message = "GobPie configuration file is not found in the project root.";
            verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Error, "Problem starting GobPie extension: " + message + " Check the output terminal of GobPie extension for more information."));
            assertTrue(systemOut.getLines().anyMatch(line -> line.contains(message)));
            assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Please add GobPie configuration file into the project root.")));
            assertEquals(expectedGobPieConfiguration, actualGobPieConfiguration);
        } catch (TimeoutException e) {
            fail("Test timeout");
        }
    }

    /**
     * Mock test to ensure @readGobPieConfiguration function reads configuration
     * when the required field is not present. After adding the required field to the
     * configuration, verify that the function successfully reads the updated configuration.
     */
    @Test
    void testGobPieConfigurationWithoutGoblintConfField() throws IOException, ExecutionException, InterruptedException {
        // Mock everything needed for creating GobPieConfReader
        MagpieServer magpieServer = mock(MagpieServer.class);
        Path tempGobpieConfFilePath = tempDir.resolve("gobpieTestTemp2.json");
        GobPieConfReader gobPieConfReader = new GobPieConfReader(magpieServer, tempGobpieConfFilePath.toString());

        GobPieConfiguration.Builder builder = new GobPieConfiguration.Builder()
                .setGoblintExecutable("/home/user/goblint/analyzer/goblint")
                .setPreAnalyzeCommand(List.of("echo", "'hello'"))
                .setAbstractDebugging(false)
                .setShowCfg(false)
                .setIncrementalAnalysis(true)
                .setExplodeGroupWarnings(false);
        GobPieConfiguration initialGobPieConfiguration = builder.createGobPieConfiguration();

        // Write the initial conf into temporary file
        Gson gson = new Gson();
        String json = gson.toJson(initialGobPieConfiguration);
        Files.write(tempGobpieConfFilePath, json.getBytes());
        assertTrue(Files.exists(tempGobpieConfFilePath));

        CompletableFuture<GobPieConfiguration> future = CompletableFuture.supplyAsync(gobPieConfReader::readGobPieConfiguration);

        GobPieConfiguration expectedGobPieConfiguration = builder.setGoblintConf("goblint.json").createGobPieConfiguration();

        // Write goblintConf field to file
        Files.write(tempGobpieConfFilePath, gson.toJson(expectedGobPieConfiguration).getBytes());

        // Assert that the configuration was read;
        // (the required field was indeed not present; the user is notified;)
        // and the configuration is successfully read when the goblintConf field is added to the file.
        try {
            GobPieConfiguration actualGobPieConfiguration = future.get(100, TimeUnit.MILLISECONDS);
            assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Reading GobPie configuration from json")));
            assertTrue(systemOut.getLines().anyMatch(line -> line.contains("GobPie configuration read from json")));
            // We cannot check the following for sure as the file can be changed too quickly
            //String message = "goblintConf parameter missing from GobPie configuration file.";
            //verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Error, "Problem starting GobPie extension: " + message + " Check the output terminal of GobPie extension for more information."));
            //assertTrue(systemOut.getLines().anyMatch(line -> line.contains(message)));
            //assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Please add Goblint configuration file location into GobPie configuration as a parameter with name \"goblintConf\"")));
            // But we can test that when readGobPieConfiguration() completes, it will always give a valid configuration
            assertEquals(expectedGobPieConfiguration, actualGobPieConfiguration);
        } catch (TimeoutException e) {
            fail("Test timeout");
        }
    }

    /**
     * Mock test to ensure @readGobPieConfiguration function
     * messages user when goblintConf parameter is missing
     * from GobPie configuration file.
     */
    @Test
    void testGobPieConfigurationWithoutGoblintConfField2() throws ExecutionException, InterruptedException {
        GobPieConfReader gobPieConfReader = preFileSetup(6);
        CompletableFuture<GobPieConfiguration> future = CompletableFuture.supplyAsync(gobPieConfReader::readGobPieConfiguration);

        // Assert that the configuration was read;
        // the required field was indeed not present;
        // and the user is notified.
        try {
            future.get(100, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // Only check for the user notification
            assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Reading GobPie configuration from json")));
            assertTrue(systemOut.getLines().anyMatch(line -> line.contains("GobPie configuration read from json")));
            String message = "goblintConf parameter missing from GobPie configuration file.";
            verify(magpieServer).forwardMessageToClient(new MessageParams(MessageType.Error, "Problem starting GobPie extension: " + message + " Check the output terminal of GobPie extension for more information."));
            assertTrue(systemOut.getLines().anyMatch(line -> line.contains(message)));
            assertTrue(systemOut.getLines().anyMatch(line -> line.contains("Please add Goblint configuration file location into GobPie configuration as a parameter with name \"goblintConf\"")));
        }
    }

}