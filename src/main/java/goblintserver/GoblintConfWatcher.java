package goblintserver;

import api.GoblintService;
import api.messages.params.Params;
import gobpie.GobPieConfiguration;
import magpiebridge.core.MagpieServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import util.FileWatcher;

import java.io.File;
import java.util.concurrent.CompletionException;

public class GoblintConfWatcher {

    private final FileWatcher fileWatcher;
    private final MagpieServer magpieServer;
    private final GoblintService goblintService;
    private final GobPieConfiguration gobpieConfiguration;

    public boolean configValid = false;

    private final Logger log = LogManager.getLogger(GoblintConfWatcher.class);

    public GoblintConfWatcher(MagpieServer magpieServer, GoblintService goblintService, GobPieConfiguration gobpieConfiguration, FileWatcher fileWatcher) {
        this.magpieServer = magpieServer;
        this.goblintService = goblintService;
        this.gobpieConfiguration = gobpieConfiguration;
        this.fileWatcher = fileWatcher;
    }

    /**
     * Reloads Goblint config if it has been changed or is currently invalid.
     */
    public boolean refreshGoblintConfig() {
        if (fileWatcher.checkModified() || !configValid) {
            configValid = goblintService.reset_config()
                    .thenCompose(_res ->
                            goblintService.read_config(new Params(new File(gobpieConfiguration.goblintConf()).getAbsolutePath())))
                    .handle((_res, ex) -> {
                        if (ex != null) {
                            Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
                            String msg = "Goblint was unable to successfully read the new configuration: " + cause.getMessage();
                            magpieServer.forwardMessageToClient(new MessageParams(MessageType.Error, msg));
                            log.error(msg);
                            return false;
                        }
                        return true;
                    })
                    .join();
        }
        return configValid;
    }
}
