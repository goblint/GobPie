

import org.eclipse.lsp4j.jsonrpc.messages.Either;

import magpiebridge.core.IProjectService;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.ServerConfiguration;
import magpiebridge.core.ToolAnalysis;
import magpiebridge.projectservice.java.JavaProjectService;

public class TutorialMain {
	public static void main(String... args) {
		
		//set up configuration for MagpieServer
		ServerConfiguration defaultConfig = new ServerConfiguration();
		MagpieServer server = new MagpieServer(defaultConfig);
		
		//define which language you consider and add a project service for this language
		String language =  "java";
		IProjectService javaProjecctService=new JavaProjectService();
		server.addProjectService(language, javaProjecctService);
		
		
		//add your customized analysis to the MagpieServer 
		String preparedFile=args[0];
		ServerAnalysis myAnalysis=new MyDummyAnalysis(preparedFile);

		Either<ServerAnalysis, ToolAnalysis> analysis=Either.forLeft(myAnalysis);
		server.addAnalysis(analysis,language);
		
		//launch the server, here we choose stand I/O. Note later don't use System.out to print text messages to console, it will block the channel.  
		server.launchOnStdio();
	}
}
