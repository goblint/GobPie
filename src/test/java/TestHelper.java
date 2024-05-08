import goblintserver.GoblintConfWatcher;
import goblintserver.GoblintServer;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class TestHelper {

    /**
     * Method to mock that GoblintServer is alive and Goblint's configuration file is ok.
     */
    static void mockGoblintServerIsAlive(GoblintServer goblintServer, GoblintConfWatcher goblintConfWatcher) {
        doReturn(true).when(goblintServer).isAlive();
        when(goblintConfWatcher.refreshGoblintConfig()).thenReturn(true);
    }
}
