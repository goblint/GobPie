package analysis;

import com.google.gson.JsonPrimitive;
import goblintclient.GoblintClient;
import goblintclient.communication.CFGResponse;
import goblintclient.communication.Request;
import gobpie.GobPieException;
import gobpie.GobPieExceptionType;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import magpiebridge.core.MagpieClient;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.WorkspaceCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.services.LanguageClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

public class ShowCFGCommand implements WorkspaceCommand {

    private final GoblintClient goblintClient;
    private final Logger log = LogManager.getLogger(ShowCFGCommand.class);

    public ShowCFGCommand(GoblintClient goblintClient) {
        this.goblintClient = goblintClient;
    }

    @Override
    public void execute(ExecuteCommandParams params, MagpieServer server, LanguageClient client) {
        try {
            String funName;
            Object uriJson = params.getArguments().get(0);
            if (uriJson instanceof JsonPrimitive) {
                funName = ((JsonPrimitive) uriJson).getAsString();
            } else {
                funName = (String) uriJson;
            }
            log.info("Showing CFG for function: " + funName);
            String cfg = getCFG(funName);
            showHTMLinClientOrBrowser(server, client, cfg);
        } catch (IOException | URISyntaxException e) {
            MagpieServer.ExceptionLogger.log(e);
            e.printStackTrace();
        }
    }

    /**
     * Writes the request to the socket to get the cfg for the given function.
     *
     * @param funName The function name for which the CFG was requested.
     * @return the CFG of the given function as a dot language string.
     * @throws GobPieException if the request and response ID do not match.
     */

    public String getCFG(String funName) {
        Request cfgRequest = new Request("cfg", funName);
        try {
            goblintClient.writeRequestToSocket(cfgRequest);
            CFGResponse cfgResponse = goblintClient.readCFGResponseFromSocket();
            if (!cfgRequest.getId().equals(cfgResponse.getId()))
                throw new GobPieException("Response ID does not match request ID.", GobPieExceptionType.GOBLINT_EXCEPTION);
            return cfgResponse.getResult().getCfg();
        } catch (IOException e) {
            throw new GobPieException("Sending the request to or receiving result from the server failed.", e, GobPieExceptionType.GOBLINT_EXCEPTION);
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
                // Generate svg from dot using graphviz-java
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                Graphviz.fromString(cfg).render(Format.SVG).toOutputStream(output);
                String svg = output.toString();
                // TODO: improve this HTML horror?
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
                                /*"       <img src=\"data:image/svg+xml;base64," + imageAsBase64 + "\" />" +*/
                                "       <svg>" + svg + "</svg>" +
                                "   </body>\n" +
                                "</html>";
                ((MagpieClient) client).showHTML(content);
            }
        } /*else {
            // TODO: Not tested if this works, probably not?
            if (Desktop.isDesktopSupported())
                Desktop.getDesktop().browse(new URI(URIUtils.checkURI(cfg)));
        }*/
    }
}

