package goblintclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import goblintclient.communication.*;
import goblintclient.messages.GoblintMessages;
import goblintclient.messages.GoblintTagInterfaceAdapter;
import gobpie.GobPieException;
import gobpie.GobPieExceptionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;


/**
 * s
 * The Class GoblintClient.
 * <p>
 * Handles the communication with Goblint Server through unix socket.
 *
 * @author Karoliine Holter
 * @since 0.0.2
 */

public class GoblintClient {

    private OutputStream outputStream;
    private BufferedReader inputReader;

    private final String goblintSocketName = "goblint.sock";

    private final Logger log = LogManager.getLogger(GoblintClient.class);


    public GoblintClient() {
    }


    /**
     * Method for connecting to Goblint server socket.
     *
     * @throws GobPieException in case Goblint socket is missing or the client was unable to connect to the socket.
     */

    public void connectGoblintClient() {
        try {
            // connect to the goblint socket
            UnixDomainSocketAddress address = UnixDomainSocketAddress.of(Path.of(goblintSocketName));
            SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
            boolean connected = channel.connect(address);
            outputStream = Channels.newOutputStream(channel);
            InputStream inputStream = Channels.newInputStream(channel);
            inputReader = new BufferedReader(new InputStreamReader(inputStream));
            if (!connected)
                throw new GobPieException("Connecting Goblint Client to Goblint socket failed.", GobPieExceptionType.GOBPIE_EXCEPTION);
            log.info("Goblint client connected.");
        } catch (IOException e) {
            throw new GobPieException("Connecting Goblint Client to Goblint socket failed.", e, GobPieExceptionType.GOBPIE_EXCEPTION);
        }
    }


    /**
     * Method for sending the requests to Goblint server.
     */

    public void writeRequestToSocket(Request request) throws IOException {
        String requestString = new GsonBuilder().create().toJson(request) + "\n";
        outputStream.write(requestString.getBytes());
        log.info("Request (" + request.getMethod() + ") " + request.getId() + " written to socket.");
    }


    /**
     * Methods for reading the responses from Goblint server.
     *
     * @return JsonObject of the results read from Goblint socket.
     */

    public AnalyzeResponse readAnalyzeResponseFromSocket() throws IOException {
        String response = inputReader.readLine();
        AnalyzeResponse analyzeResponse = new Gson().fromJson(response, AnalyzeResponse.class);
        log.info("Response (analyze) " + analyzeResponse.getId() + " read from socket.");
        return analyzeResponse;
    }


    public MessagesResponse readMessagesResponseFromSocket() throws IOException {
        String response = inputReader.readLine();
        GsonBuilder builder = new GsonBuilder();
        // Add deserializer for tags
        builder.registerTypeAdapter(GoblintMessages.tag.class, new GoblintTagInterfaceAdapter());
        Gson gson = builder.create();
        MessagesResponse messagesResponse = gson.fromJson(response, MessagesResponse.class);
        log.info("Response (messages) " + messagesResponse.getId() + " read from socket.");
        return messagesResponse;
    }


    public FunctionsResponse readFunctionsResponseFromSocket() throws IOException {
        String response = inputReader.readLine();
        FunctionsResponse functionsResponse = new Gson().fromJson(response, FunctionsResponse.class);
        log.info("Response " + functionsResponse.getId() + " read from socket.");
        return functionsResponse;
    }


    public CFGsResponse readCFGsResponseFromSocket() throws IOException {
        String response = inputReader.readLine();
        CFGsResponse cfgsResponse = new Gson().fromJson(response, CFGsResponse.class);
        log.info("Response " + cfgsResponse.getId() + " read from socket.");
        return cfgsResponse;
    }


}
