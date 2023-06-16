package analysis;

import com.google.common.io.CharStreams;
import com.google.gson.JsonPrimitive;
import magpiebridge.core.MagpieClient;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.WorkspaceCommand;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.services.LanguageClient;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ShowCFGCommand implements WorkspaceCommand {

    private final String httpServerAddress;
    private final Logger log = LogManager.getLogger(ShowCFGCommand.class);

    public ShowCFGCommand(String httpServerAddress) {
        this.httpServerAddress = httpServerAddress;
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
            showHTMLinClientOrBrowser(server, client, funName);
        } catch (IOException e) {
            // Note: Logging the exception using log.error("Error:", e) causes a strange bug where all subsequent calls to
            // MagpieClient.showMessage, MagpieClient.showHTML and possibly other MagpieClient methods do nothing and produce no error.
            // TODO: Figure out why and how this happens and create/link relevant issues.
            log.error("Error running showcfg command:");
            e.printStackTrace();
        }
    }


    /**
     * Show A HTML page with the given CFG in the client, or in a browser if the client doesn't support this.
     *
     * @param server  The MagpieServer
     * @param client  The IDE/Editor
     * @param funName The name of the function to show the CFG for
     * @throws IOException IO exception
     */

    public void showHTMLinClientOrBrowser(MagpieServer server, LanguageClient client, String funName) throws IOException {
        if (server.clientSupportShowHTML()) {
            if (client instanceof MagpieClient) {
                String json = "{\"funName\": \"" + funName + "\"}";
                String content = httpPostJson(httpServerAddress + "cfg/", json);
                ((MagpieClient) client).showHTML(content);
            }
        } /*else {
            // TODO: Not tested if this works, probably not?
            if (Desktop.isDesktopSupported())
                Desktop.getDesktop().browse(new URI(URIUtils.checkURI(cfg)));
        }*/
    }


    /**
     * Sends the request to get the HTML for visualizing the CFG.
     *
     * @param url  The HTTPServer url to post the request to.
     * @param json The request body as a JSON.
     * @return The response body as a string.
     * @throws IOException in case of a problem or the connection was aborted.
     */

    public static String httpPostJson(String url, String json) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        HttpResponse response = httpClient.execute(httpPost);
        String responseBody = CharStreams.toString(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
        httpClient.close();
        return responseBody;
    }


}

