import com.google.gson.JsonPrimitive;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.WorkspaceCommand;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;

public class ShowResultCommand implements WorkspaceCommand  {
    @Override
    public void execute(ExecuteCommandParams params, MagpieServer server, LanguageClient client) {
        JsonPrimitive line = (JsonPrimitive) params.getArguments().get(0);
        server.forwardMessageToClient(new MessageParams(MessageType.Info, "Showing result for position: " + line));
    }
}
