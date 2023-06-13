package HTTPserver;

import api.GoblintService;
import api.messages.params.NodeParams;
import api.messages.params.Params;
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

/**
 * The Class GobPieHttpHandler.
 * <p>
 * Implements the class {@link HttpHandler}.
 * Handles the requests sent to the HTTP server.
 *
 * @author Karoliine Holter
 * @since 0.0.3
 */

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

        TemplateEngine templateEngine = createTemplateEngine("/templates/", ".html", TemplateMode.HTML);
        Context context = new Context();

        if (exchange.getRequestMethod().equalsIgnoreCase("post")) {
            response = switch (path) {
                case "/cfg/" -> {
                    String funName = readRequestBody(is).get("funName").getAsString();
                    context.setVariable("cfgSvg", getCFG(funName));
                    context.setVariable("url", httpServerAddress + "node/");
                    context.setVariable("jsonTreeCss", httpServerAddress + "static/jsonTree.css/");
                    context.setVariable("jsonTreeJs", httpServerAddress + "static/jsonTree.js/");
                    log.info("Showing CFG for function: " + funName);
                    yield templateEngine.process("base", context);
                }
                case "/node/" -> {
                    String nodeId = readRequestBody(is).get("node").getAsString();
                    List<JsonObject> states = getNodeStates(nodeId);
                    log.info("Showing state info for node with ID: " + nodeId);
                    yield states.get(0).toString();
                }
                default -> templateEngine.process("index", context);
            };
        } else {
            response = switch (path) {
                case "/static/jsonTree.css/" -> {
                    templateEngine = createTemplateEngine("/json-viewer/", ".css", TemplateMode.CSS);
                    yield templateEngine.process("jquery.json-viewer", context);
                }
                case "/static/jsonTree.js/" -> {
                    templateEngine = createTemplateEngine("/json-viewer/", ".js", TemplateMode.JAVASCRIPT);
                    yield templateEngine.process("jquery.json-viewer", context);
                }
                default -> templateEngine.process("index", context);
            };
        }
        exchange.sendResponseHeaders(HTTP_OK_STATUS, response.getBytes().length);
        writeResponse(os, response);
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
    private TemplateEngine createTemplateEngine(String prefix, String suffix, TemplateMode templateMode) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode(templateMode);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setPrefix(prefix);
        resolver.setSuffix(suffix);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        return templateEngine;
    }


    /**
     * Sends the request to get the CFG for the given function,
     * converts the CFG to a svg and returns it.
     *
     * @param funName The function name for which the CFG was requested. If function name is {@code "<arg>"} requests the ARG instead.
     * @return The CFG of the given function as a svg.
     * @throws GobPieException if the request and response ID do not match.
     */
    private String getCFG(String funName) {
        Params params = new Params(funName);
        try {
            String cfg;
            if (funName.equals("<arg>")) {
                cfg = goblintService.arg_dot().get().getArg();
            } else {
                cfg = goblintService.cfg_dot(params).get().getCfg();
            }
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
     * @throws GobPieException
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
            throw new GobPieException("Converting dot language string to svg failed.", e, GobPieExceptionType.GOBPIE_EXCEPTION);
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
        NodeParams params = new NodeParams(nodeId);
        try {
            return goblintService.cfg_state(params).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new GobPieException("Sending the request to or receiving result from the server failed.", e, GobPieExceptionType.GOBLINT_EXCEPTION);
        }
    }

}
