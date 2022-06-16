package analysis;

import com.google.gson.JsonPrimitive;
import magpiebridge.core.MagpieClient;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.WorkspaceCommand;
import magpiebridge.util.URIUtils;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.services.LanguageClient;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class ShowCFGCommand implements WorkspaceCommand {

    @Override
    public void execute(ExecuteCommandParams params, MagpieServer server, LanguageClient client) {
        try {
            String uri;
            Object uriJson = params.getArguments().get(0);
            if (uriJson instanceof JsonPrimitive) {
                uri = ((JsonPrimitive) uriJson).getAsString();
            } else {
                uri = (String) uriJson;
            }
            showHTMLinClientOrBrowser(server, client, uri);
        } catch (IOException | URISyntaxException e) {
            MagpieServer.ExceptionLogger.log(e);
            e.printStackTrace();
        }
    }


    /**
     * Show A HTML page with the given CFG in the client, or in a browser if the client doesn't support this.
     *
     * @param server The MagpieServer
     * @param client The IDE/Editor
     * @param cfg    The CFG which should be shown
     * @throws IOException        IO exception
     * @throws URISyntaxException URI exception
     */
    public static void showHTMLinClientOrBrowser(MagpieServer server, LanguageClient client, String cfg) throws IOException, URISyntaxException {
        if (server.clientSupportShowHTML()) {
            if (client instanceof MagpieClient) {
                String content =
                        "<!DOCTYPE html>\n" +
                                "<html lang=\"en\"\">\n" +
                                "   <head>\n" +
                                "       <meta charset=\"UTF-8\">\n" +
                                "       <title>Preview</title>\n" +
                                "       <style>\n" +
                                "          html { width: 100%; height: 100%; min-height: 100%; display: flex; }\n" +
                                "          body { flex: 1; display: flex; }\n" +
                                "          iframe { flex: 1; border: none; background: white; }\n" +
                                "       </style>\n" +
                                "   </head>\n" +
                                "   <body>\n" +
                                "       <iframe src=\"" + cfg + "\"></iframe>\n" +
                                "   </body>\n" +
                                "</html>";
                ((MagpieClient) client).showHTML(content);
            }
        } else {
            // TODO: probably does not work?
            if (Desktop.isDesktopSupported())
                Desktop.getDesktop().browse(new URI(URIUtils.checkURI(cfg)));
        }
    }
}

