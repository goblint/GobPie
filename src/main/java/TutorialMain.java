
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import magpiebridge.core.IProjectService;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.ServerConfiguration;
import magpiebridge.core.ToolAnalysis;
import magpiebridge.projectservice.java.JavaProjectService;

public class TutorialMain {
  public static void main(String... args) {

    String preparedFile = args[0];

    // launch the server, here we choose stand I/O. Note later don't use System.out
    // to print text messages to console, it will block the channel.
    createServer(preparedFile).launchOnStdio();

    // for debugging
    //MagpieServer.launchOnSocketPort(5007, () -> createServer(preparedFile));
  }

  private static MagpieServer createServer(String preparedFile) {
    // set up configuration for MagpieServer
    ServerConfiguration defaultConfig = new ServerConfiguration();
    MagpieServer server = new MagpieServer(defaultConfig);
    // define which language you consider and add a project service for this
    // language
    String language = "java";
    IProjectService javaProjectService = new JavaProjectService();

    // add your customized analysis to the MagpieServer
    ServerAnalysis myAnalysis = new MyDummyAnalysis(preparedFile);
    server.addProjectService(language, javaProjectService);

    Either<ServerAnalysis, ToolAnalysis> analysis = Either.forLeft(myAnalysis);
    server.addAnalysis(analysis, language);
    return server;
  }
}
