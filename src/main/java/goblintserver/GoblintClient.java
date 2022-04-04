package goblintserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Class GoblintClient.
 * 
 * Handles the communication with Goblint Server through unix socket.
 * 
 * @author      Karoliine Holter
 * @since       0.0.2
 */

public class GoblintClient {

    private SocketChannel channel;
    private UnixDomainSocketAddress address;
    private OutputStream outputStream;
    private BufferedReader inputReader;
    
    private final String goblintSocketName = "goblint.sock";

    private final Logger log = LogManager.getLogger(GoblintClient.class);


    public GoblintClient() {}


    /**
     * Method for connecting to Goblint server socket.
     *
     * @throws GobPieException in case Goblint socket is missing or the client was unable to connect to the socket.
     */

    public void connectGoblintClient() {
        try {
            // connect to the goblint socket
            address = UnixDomainSocketAddress.of(Path.of(goblintSocketName));
            channel = SocketChannel.open(StandardProtocolFamily.UNIX);
            boolean connected = channel.connect(address);
            outputStream = Channels.newOutputStream(channel);
            InputStream inputStream = Channels.newInputStream(channel);
            inputReader = new BufferedReader(new InputStreamReader(inputStream));
            if (!connected) throw new GobPieException("Connecting Goblint Client to Goblint socket failed.", GobPieExceptionType.GOBPIE_EXCEPTION);
            log.info("Goblint client connected.");
        } catch (IOException e) {
            throw new GobPieException("Connecting Goblint Client to Goblint socket failed.", e, GobPieExceptionType.GOBPIE_EXCEPTION);
        }
    }


    /**
     * Method for sending the requests to Goblint server.
     */

    public void writeRequestToSocket(String request) throws IOException {
        outputStream.write(request.getBytes());    
        log.info("Request written to socket.");
    }


    /**
     * Method for reading the response from Goblint server.
     * @return JsonObject of the results read from Goblint socket.
     */

    public JsonObject readResponseFromSocket() throws IOException {
        String response = inputReader.readLine();
        log.info("Response read from socket.");
        JsonObject responseJson = new Gson().fromJson (response, JsonElement.class).getAsJsonObject();
        return responseJson;
    }

}
