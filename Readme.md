In this tutorial, you will learn how you can use MagpieBridge to present any results in IDEs. VS Code is used for this tutorial, please install it in advance.

The **goal** of this tutorial is to use MagpieBridge to write a language server which displays results read from a JSON file in a code editor when the DemoProject is opened. The [DemoProject](https://github.com/MagpieBridge/DemoProject) is a Java project and it contains an insecure API usage. The [prepared JSON file](https://github.com/MagpieBridge/Tutorial2/blob/master/vscode/preparedResults.json) contains the description of the insecure usage, the source code position and how to repair the code. You can image such JSON file is computed by a program analysis tool. Of course, you can provide the results in any other file formats or compute them at run time.

[This screenshot](https://github.com/MagpieBridge/Tutorial2/blob/master/gitpod.png) shows you the expected behavior of the desired language server when you interact with the code editor. 

Please check out the following projects:

- Tutorial2: https://github.com/MagpieBridge/Tutorial2 

    This is the implementation of the desired language server.

- DemoProject: https://github.com/MagpieBridge/DemoProject 

    This is the demo project for testing the language server of Tutorial2. 

## Do it by yourself

1. Create an empty maven project.  
2. Add MagpieBridge and Gson into the pom.xml of your project (see [here](https://github.com/MagpieBridge/Tutorial2/blob/master/pom.xml)).

3. Create a mainClass called [TutorialMain.java](https://github.com/MagpieBridge/Tutorial2/blob/master/src/main/java/TutorialMain.java) in your project and add `maven-shade-plugin` to the pom.xml and speicify the mainClass under configuration (see [here](https://github.com/MagpieBridge/Tutorial2/blob/master/pom.xml)). This is used to create a JAR file containing every dependency you need for running your language server. 
~~~
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
	<artifactId>maven-shade-plugin</artifactId>
	<version>2.3</version>
	<executions>
		<execution>
		    <phase>package</phase>
			<goals>
				<goal>shade</goal>
			</goals>
				<configuration>
					...
						    <mainClass>TutorialMain</mainClass>
						</transformer>
					</transformers>
....
</plugin>
~~~

4.  Set up MagpieServer in the main method of TutorialMain.java

   
~~~
        // initialize a MagpieServer with default configuration. 
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
		
		//launch the server, here we choose stand I/O. 
        //Note later don't use System.out to print text messages to console, it will block the channel.  
		server.launchOnStdio();
	
		//for debugging 
		//server.launchOnSocketPort(5007);
~~~

5. Create [MyDummyAnalysis.java](https://github.com/MagpieBridge/Tutorial2/blob/master/src/main/java/MyDummyAnalysis.java) which implements the ServerAnalysis interface.
~~~
public class MyDummyAnalysis implements ServerAnalysis{

	@Override
	public String source() {
		// TODO 
		return null;
	}

	@Override
	public void analyze(Collection<? extends Module> files, MagpieServer server, boolean rerun) {
		// TODO 
	}

~~~
In the default configuration for MagpieServer, the method `analyze` will be called every time a source code file with desired langauge (in this project Java) is opened/saved in a code editor. 
Meaning of the parameters: 
 - `files`: the opened source code files in the editor sent to MagpieServer.  
 - `server`: the MagpieServer which calls this method, you can get project scope (source code path, library path, class path etc.) by calling `server.getProjectService(language)`
 - `rerun`: the flag to indicate if the analysis should be reran. 

6. In [MyDummyAnalysis.java](https://github.com/MagpieBridge/Tutorial2/blob/master/src/main/java/MyDummyAnalysis.java), create a method `runAnalysisOnSelectedFiles` which returns a set of `AnalysisResult`. This method should look every opened source file and check if there is a result reading from a prepared JSON file available for this source file. In this project, the [prepared JSON file](https://github.com/MagpieBridge/Tutorial2/blob/master/vscode/preparedResults.json) contains the following result for Example.java:
~~~
{   
    "results":[
        {
            "fileName": "Example.java",  
            "msg": "Insecure parameter! The first ...",
            "startLine": 11,
            "startColumn": 43,
			"endLine":11,
			"endColumn":63,
            "repair":"AES/CBC/PKCS5Padding"
        }
    ]
 }
 ~~~
All above information can be used for implementing the `ÀnalysisResult` interface. 

7. Call `runAnalysisOnSelectedFiles` and consumes the results in the method `analyze` in [MyDummyAnalysis.java](https://github.com/MagpieBridge/Tutorial2/blob/master/src/main/java/MyDummyAnalysis.java)
~~~
@Override
public void analyze(Collection<? extends Module> files, MagpieServer server, boolean rerun) {
	if (rerun) {
		Set<AnalysisResult> results = runAnalysisOnSelectedFiles(files, server);
		server.consume(results, source());
    }
}
~~~

8. In the project root, run `mvn install` to build a JAR file `tutorial2-0.0.1-SNAPSHOT.jar` of your project. This JAR file can be used as a language server in IDEs with `java -jar tutorial2-0.0.1-SNAPSHOT.jar PATH\TO\preparedResults.json`(see step 10 upwards in [Tutorial1](https://github.com/MagpieBridge/MagpieBridge/wiki/Tutorial-1.-Create-your-first-project-with-MagpieBridge-for-soot-based-analysis)).

9. After configuring this JAR file as a language server, try it with the DemoProject by opening the Example.java file in an editor. 

10. Try it on the [DemoProject with Gitpod](https://gitpod.io/#https://github.com/MagpieBridge/DemoProject) in your browser, you should see the following result when you open Example.java under tutorial2 package
<img src="https://github.com/MagpieBridge/Tutorial2/blob/master/gitpod.png" width="800">

## Debugging it in VS Code. 
- Copy the [vscode](https://github.com/MagpieBridge/Tutorial2/tree/master/vscode) folder into the project root.
- Server configuration:  Instead of calling `server.launchOnStdio()` in the main method, calling `server.launchOnSocketPort>(5007)`
- Client configuration(VS Code): Define communication via socket in `extension.ts` with the same port 5007
~~~
 let serverOptions = () => {
		const socket = net.connect({ port: 5007 })
		const result: StreamInfo = {
			writer: socket,
			reader: socket
		}
		return new Promise<StreamInfo>((resolve) => {
			socket.on("connect", () => resolve(result))
			socket.on("error", _ =>
				window.showErrorMessage(
					"Failed to connect to TaintBench language server. Make sure that the language server is running " +
					"-or- configure the extension to connect via standard IO."))
		})
~~~
- build jar file with `mvn install`
- make sure you can call the command `cp` (copy) in your machine, since in [package.json](https://github.com/MagpieBridge/Tutorial2/blob/master/vscode/package.json), the following script is used before compile the vscode extension: 
~~~
"vscode:prepublish": "cp ../target/tutorial2-0.0.1-SNAPSHOT.jar  tutorial2-0.0.1-SNAPSHOT.jar && npm run compile"
~~~
If you don't have `cp`, simply copy the Jar file into the vscode folder and change the script to:
~~~
"vscode:prepublish": "npm run compile"
~~~
- in vscode folder run:
~~~
        - npm install (if the first time)
        - npm install -g vsce (if the first time)
        - vsce package (this will create vscode extension under vscode directory)
~~~
- Start TutorialMain at first with argument "vscode/preparedResults.json" in your preferred editor. 
- Debug and run the extension in VS Code. This will open a new vs code instance with the extension installed. In this instance, open the DemoProject. Open the Example.java file.  

**Watch this video to see the demonstration**

  [![Tutorial2](https://img.youtube.com/vi/GZ0VfA7WvTs/0.jpg)](https://youtu.be/GZ0VfA7WvTs)
