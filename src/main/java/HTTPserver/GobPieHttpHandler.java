package HTTPserver;

import api.GoblintService;
import api.messages.GoblintCFGResult;
import api.messages.Params;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import gobpie.GobPieException;
import gobpie.GobPieExceptionType;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class GobPieHttpHandler implements HttpHandler {

    private static final int HTTP_OK_STATUS = 200;
    private final String httpServerAddress;
    private final GoblintService goblintService;

    private final Logger log = LogManager.getLogger(GobPieHttpHandler.class);

    public GobPieHttpHandler(String httpServerAddress, GoblintService goblintService) {
        this.httpServerAddress = httpServerAddress;
        this.goblintService = goblintService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String path = exchange.getRequestURI().getPath();
        OutputStream os = exchange.getResponseBody();
        InputStream is = exchange.getRequestBody();
        String response;

        // CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        TemplateEngine templateEngine = createTemplateEngine();
        Context context = new Context();

        switch (path) {
            case "/":
                response = templateEngine.process("index", context);
                exchange.sendResponseHeaders(HTTP_OK_STATUS, response.getBytes().length);
                writeResponse(os, response);
                break;
            case "/cfg/":
                String funName = readRequestBody(is).get("funName").getAsString();
                context.setVariable("cfgSvg", getCFG(funName));
                context.setVariable("url", httpServerAddress + "node/");
                log.info("Showing CFG for function: " + funName);

                response = templateEngine.process("base", context);
                exchange.sendResponseHeaders(HTTP_OK_STATUS, response.getBytes().length);
                writeResponse(os, response);
                break;
            case "/node/":
                List<JsonObject> states = getNodeStates(readRequestBody(is).get("node").getAsString());

                response = states.get(0).toString();
                exchange.sendResponseHeaders(HTTP_OK_STATUS, response.getBytes().length);
                writeResponse(os, response);
                break;
        }

    }

    private JsonObject readRequestBody(InputStream is) {
        return JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private void writeResponse(OutputStream os, String response) throws IOException {
        os.write(response.getBytes());
        os.flush();
        os.close();
    }


    /**
     * Creates the Thymeleaf Template Engine
     * for accessing the templates from resources/templates.
     *
     * @return TemplateEngine instance
     */
    private TemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        return templateEngine;
    }


    /**
     * Sends the request to get the CFG for the given function,
     * converts the CFG to a svg and returns it.
     *
     * @param funName The function name for which the CFG was requested.
     * @return The CFG of the given function as a svg.
     * @throws GobPieException if the request and response ID do not match.
     */
    private String getCFG(String funName) {
        Params params = new Params(funName);
        try {
            GoblintCFGResult cfgResponse = goblintService.cfg(params).get();
            String cfg = cfgResponse.getCfg();
            return cfg2svg(cfg);
        } catch (ExecutionException | InterruptedException e) {
            throw new GobPieException("Sending the request to or receiving result from the server failed.", e, GobPieExceptionType.GOBLINT_EXCEPTION);
        }
    }

    /**
     * Converts the dot language string to a svg.
     *
     * @param cfg The CFG as a dot language string.
     * @return The CFG of the given function as a svg.
     * @throws GobPieException TODO: description
     */

    private String cfg2svg(String cfg) {
        try {
            // Generate svg from dot using graphviz-java
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Graphviz.fromString(cfg).render(Format.SVG).toOutputStream(output);
            String svg = output.toString();
            // TODO: figure out something else instead of the following replace ugliness
            return svg.replaceAll("xlink:href=\"javascript:", "onclick=\"");
        } catch (IOException e) {
            // TODO: meaningful error message
            throw new GobPieException("", e, GobPieExceptionType.GOBPIE_EXCEPTION);
        }
    }


    /**
     * Sends the request to get the state info for the given CFG node.
     *
     * @param nodeId The id of the node for which the states are requested.
     * @return A list of json objects expressing the state.
     * @throws GobPieException if the request and response ID do not match.
     */
    private List<JsonObject> getNodeStates(String nodeId) {
        Params params = new Params(nodeId);
        try {
            return goblintService.node_state(params).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new GobPieException("Sending the request to or receiving result from the server failed.", e, GobPieExceptionType.GOBLINT_EXCEPTION);
        }
    }

}
