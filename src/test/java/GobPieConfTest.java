import analysis.GoblintAnalysis;
import api.GoblintService;
import api.messages.params.AnalyzeParams;
import com.ibm.wala.classLoader.Module;
import goblintserver.GoblintConfWatcher;
import goblintserver.GoblintServer;
import gobpie.GobPieConfReader;
import gobpie.GobPieConfiguration;
import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.MagpieServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.junit.jupiter.api.Test;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.process.UnixProcess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class GobPieConfTest {


    @Test
    void testParseGobPieConf() {

        //test fail, konstruktori test faili nimi

        // Mock everything needed for creating GoblintAnalysis
        MagpieServer magpieServer = mock(MagpieServer.class);
        GoblintServer goblintServer = mock(GoblintServer.class);
        GoblintService goblintService = mock(GoblintService.class);
        GobPieConfiguration gobPieConfiguration = mock(GobPieConfiguration.class);
        GoblintConfWatcher goblintConfWatcher = mock(GoblintConfWatcher.class);


        GoblintAnalysis goblintAnalysis = new GoblintAnalysis(magpieServer, goblintServer, goblintService, gobPieConfiguration, goblintConfWatcher);


        //GobPieConfReader gobPieConfReader = mock(GobPieConfReader.class);
        //GobPieConfiguration gobPieConfigurationTest = gobPieConfReader.parseGobPieConf(); // See returnib praegu null´'i .. ?

        // Kas saan assertida mocki vastu? - isegi kui testObj poleks null, siis poleks ju ka Mock.
        // kust tean mille vastu assertida?
        //assertEquals(gobPieConfiguration, gobPieConfigurationTest);

    }

}
