import api.json.GoblintMessageJsonHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import java.io.IOException;
import static org.mockito.Mockito.mock;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

//ExtendWith(MockServerExtension.class)
//@MockServerSettings(ports = {8888})
public class JsonFormatTest {

    @Mock
    GoblintMessageJsonHandler goblintMessageJsonHandler = mock(GoblintMessageJsonHandler.class);

    private final ClientAndServer client;

    private MockServerClient mockServerClient;

    public JsonFormatTest(ClientAndServer client) {
        this.client = client;
    }


    @Test
    void TestConvertMessagesFromJson() throws IOException {

        try (ClientAndServer mockServer = new ClientAndServer(1080)) {
            var respond = new MockServerClient("localhost", 1080)
                    .when(
                            request()
                                    .withMethod("REQUEST")
                                    .withBody("{\"jsonrpc\":\"2.0\",\"id\":0,\"method\":\"messages\"}")
                    )
                    .respond(
                            response()
                                    .withStatusCode(302)
                    );
        }
        ;
    }


}
