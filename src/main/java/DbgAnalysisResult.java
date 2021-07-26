import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IMethod;

import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;
import magpiebridge.util.SourceCodeReader;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;


public class DbgAnalysisResult implements AnalysisResult {

    String message;
    final Position pos;


    public DbgAnalysisResult(DbgResult dbgresult,  URL sourcefileUrl) {
        this.message = dbgresult.message;
        // specify the exact columns where the assertion starts and ends
        this.pos = new Position() {

            @Override
            public int getFirstCol() {
                return dbgresult.columnStart;
            }

            @Override
            public int getFirstLine() {
                return dbgresult.linenumber;
            }

            @Override
            public int getFirstOffset() {
                return 0;
            }

            @Override
            public int getLastCol() {
                return dbgresult.columnEnd;
            }

            @Override
            public int getLastLine() {
                return dbgresult.linenumber;
            }

            @Override
            public int getLastOffset() {
                return 0;
            }

            @Override
            public int compareTo(IMethod.SourcePosition arg0) {
                return 0;
            }

            @Override
            public Reader getReader() {
                return null;
            }

            @Override
            public URL getURL() {
                return sourcefileUrl;
            }
        };
    }

    @Override
    public Kind kind() {
        return Kind.Diagnostic;
    }

    @Override
    public String toString(boolean useMarkdown) {
        return message;
    }

    @Override
    public CAstSourcePositionMap.Position position() {
        return this.pos;
    }

    @Override
    public Iterable<Pair<CAstSourcePositionMap.Position, String>> related() {
        return new ArrayList<>();
    }

    @Override
    public DiagnosticSeverity severity() {
            DiagnosticSeverity severity = DiagnosticSeverity.Information;
            if (message.contains("unknown")) {
                severity = DiagnosticSeverity.Warning;
            } else if (message.contains("fail")) {
                severity = DiagnosticSeverity.Error;
            }
        return severity;
    }

    @Override
    public Pair<CAstSourcePositionMap.Position, String> repair() {
        // suggest a repair if available
        return null;
    }

    @Override
    public String code() {
        String code;
        try {
            code = SourceCodeReader.getLinesInString(pos);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return code;
    }
}
