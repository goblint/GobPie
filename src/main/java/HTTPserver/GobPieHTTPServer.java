package HTTPserver;

import api.GoblintService;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;

public class GobPieHTTPServer {
    private HttpServer httpServer;

    private String httpServerAddress;
    private final Logger log = LogManager.getLogger(GobPieHTTPServer.class);

    public GobPieHTTPServer(GoblintService goblintService) {
        try {
            InetSocketAddress socket = new InetSocketAddress("0.0.0.0", 0);
            httpServer = HttpServer.create(socket, 42);
            /*System.out.println(Arrays.toString());
            Inet4Address.getAllByName()*/


            httpServerAddress = "http://localhost:" + this.httpServer.getAddress().getPort() + "/";
            httpServer.createContext("/", new GobPieHttpHandler(httpServerAddress, goblintService));
            httpServer.createContext("/cfg/", new GobPieHttpHandler(httpServerAddress, goblintService));
            httpServer.createContext("/node/", new GobPieHttpHandler(httpServerAddress, goblintService));

            httpServer.setExecutor(null);
        } catch (IOException e) {
            log.error(e.getStackTrace());
        }
    }

    public String start() {
        httpServer.start();
        log.info("HTTP server started on: " + httpServerAddress);
        return httpServerAddress;
    }

}
