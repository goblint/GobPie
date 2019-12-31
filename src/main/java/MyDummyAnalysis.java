
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.lsp4j.DiagnosticSeverity;

import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.util.collections.Pair;

import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.projectservice.java.JavaProjectService;
import magpiebridge.util.SourceCodeReader;

public class MyDummyAnalysis implements ServerAnalysis {

	private String pathToFindResults;
	private Set<MyDummyResult> myResults;

	public MyDummyAnalysis(String pathToFindResults) {
		this.pathToFindResults = pathToFindResults;
		this.myResults = new HashSet<>();
		readResults();
	}

	private void readResults() {
		try {
			JsonObject obj = JsonParser.parseReader(new FileReader(new File(pathToFindResults))).getAsJsonObject();
			JsonArray results = obj.get("results").getAsJsonArray();
			for (int i = 0; i < results.size(); i++) {
				JsonObject result = results.get(i).getAsJsonObject();
				MyDummyResult res = new MyDummyResult();
				res.fileName = result.get("fileName").getAsString();
				res.msg = result.get("msg").getAsString();
				res.startLine = result.get("startLine").getAsInt();
				res.startColumn = result.get("startColumn").getAsInt();
				res.endLine = result.get("endLine").getAsInt();
				res.endColumn = result.get("endColumn").getAsInt();
				res.repair = result.get("repair").getAsString();
				myResults.add(res);
			}

		} catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
			throw new RuntimeException(e);
		}

	}

	public String source() {
		return "MyDummyAnalysis";
	}

	@Override
	public void analyze(Collection<? extends Module> files, MagpieServer server, boolean rerun) {
		try {
			if (rerun) {
				Set<AnalysisResult> results = runAnalysisOnSelectedFiles(files, server);
				server.consume(results, source());
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

	}

	public Set<AnalysisResult> runWholeProgramAnalysis(MagpieServer server) {
		Set<AnalysisResult> results = new HashSet<>();
		JavaProjectService ps = (JavaProjectService) server.getProjectService("java").get();
		Optional<Path> rootPath = ps.getRootPath();
		Set<Path> classPath = ps.getClassPath();
		Set<Path> libraryPath = ps.getLibraryPath();
		Set<Path> sourcePath = ps.getSourcePath();
		// TODO. run your analysis here with all the path information you get from
		// server.
		
		return results;
	}

	public Set<AnalysisResult> runAnalysisOnSelectedFiles(Collection<? extends Module> files, MagpieServer server)
			throws MalformedURLException {
		Set<AnalysisResult> results = new HashSet<>();
		for (Module file : files) {
			if (file instanceof SourceFileModule) {
				SourceFileModule sourceFile = (SourceFileModule) file;
				for (MyDummyResult res : this.myResults) {
					String className = sourceFile.getClassName();
					if (res.fileName.equals(className + ".java")) {
						final MyDummyResult result = res;
						//Note: the URL getting from files is at the server side, 
						//you need to get client (the code editor) side URL for client to consume the results. 
						final URL clientURL = new URL(server.getClientUri(sourceFile.getURL().toString()));
						final Position pos = new Position() {

							@Override
							public int getFirstCol() {
								return result.startColumn;
							}

							@Override
							public int getFirstLine() {
								return result.startLine;
							}

							@Override
							public int getFirstOffset() {
								return 0;
							}

							@Override
							public int getLastCol() {
								return result.endColumn;
							}

							@Override
							public int getLastLine() {
								return result.endLine;
							}

							@Override
							public int getLastOffset() {
								return 0;
							}

							@Override
							public int compareTo(SourcePosition arg0) {
								return 0;
							}

							@Override
							public Reader getReader() throws IOException {
								return null;
							}

							@Override
							public URL getURL() {
								return clientURL;
							}
						};
						AnalysisResult r = convert(result, pos);
						results.add(r);
					}
				}
			}
		}
		return results;
	}

	private AnalysisResult convert(final MyDummyResult result, final Position pos) {
		return new AnalysisResult() {

			@Override
			public String toString(boolean useMarkdown) {
				return result.msg;
			}

			@Override
			public DiagnosticSeverity severity() {
				return DiagnosticSeverity.Error;
			}

			@Override
			public Pair<Position, String> repair() {
				return Pair.make(pos, result.repair);
			}

			@Override
			public Iterable<Pair<Position, String>> related() {
				return new ArrayList<Pair<Position,String>>();
			}

			@Override
			public Position position() {
				return pos;
			}

			@Override
			public Kind kind() {
				return Kind.Diagnostic;
			}

			@Override
			public String code() {
				String code = null;
				try {
					code = SourceCodeReader.getLinesInString(pos);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return code;
			}
		};
	}
}
