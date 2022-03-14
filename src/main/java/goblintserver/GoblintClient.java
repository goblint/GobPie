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
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;

import magpiebridge.core.MagpieServer;


public class GoblintClient {

    private MagpieServer magpieServer;
    private SocketChannel channel;
    private UnixDomainSocketAddress address;
    private OutputStream outputStream;
    private BufferedReader inputReader;
    
    private final String goblintSocketName = "goblint.sock";

    private final Logger log = LogManager.getLogger(GoblintClient.class);


    public GoblintClient(MagpieServer magpieServer) {
        this.magpieServer = magpieServer;
    }


    /**
     * Method for connecting to Goblint server socket.
     *
     * @return True if connection was started successfully, false otherwise.
     */

    public boolean connectGoblintClient() {
        try {
            // connect to the goblint socket
            address = UnixDomainSocketAddress.of(Path.of(goblintSocketName));
            channel = SocketChannel.open(StandardProtocolFamily.UNIX);
            boolean connected = channel.connect(address);
            outputStream = Channels.newOutputStream(channel);
            InputStream inputStream = Channels.newInputStream(channel);
            inputReader = new BufferedReader(new InputStreamReader(inputStream));
            if (!connected) return false;
            log.info("Goblint client connected.");
            return true;
        } catch (IOException e) {
            this.magpieServer.forwardMessageToClient(new MessageParams(MessageType.Error, "Connecting GoblintClient failed. " + e.getMessage()));
            return false;
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
     */

    public JsonObject readResponseFromSocket() throws IOException {
        String response = inputReader.readLine();
        log.info("Response read from socket.");
        JsonObject responseJson = new Gson().fromJson (response, JsonElement.class).getAsJsonObject();
        return responseJson;
    }

}
