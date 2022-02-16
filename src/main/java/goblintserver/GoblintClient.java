package goblintserver;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;

import magpiebridge.core.MagpieServer;


public class GoblintClient {

    private MagpieServer magpieServer;
    private SocketChannel channel;
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

    public boolean connectGoblitClient() {
        try {
            // connect to the goblint socket
            UnixDomainSocketAddress address = UnixDomainSocketAddress.of(Path.of(goblintSocketName));
            channel = SocketChannel.open(StandardProtocolFamily.UNIX);
            boolean connected = channel.connect(address);
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

    public void writeMessageToSocket(String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.clear();
        buffer.put(message.getBytes());
        buffer.flip();
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
        log.debug("Message written to socket.");
    }

}
