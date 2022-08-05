package goblintclient;

/**
 * The Class {@link GoblintClient}.
 * <p>
 * The local endpoint.
 *
 * @author Karoliine Holter
 * @since 0.0.2
 */

public class GoblintClient {

    private GoblintService server;

    public void connect(GoblintService server) {
        this.server = server;
    }

    public GoblintService getServer() {
        if (this.server == null) {
            throw new IllegalStateException("not connected");
        }
        return this.server;
    }

}
