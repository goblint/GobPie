import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;
import magpiebridge.util.SourceCodeReader;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;


public class DbgResult implements AnalysisResult {

    DiagnosticSeverity severity;
    String message;
    final Position pos;


    public DbgResult(int linenumber, URL sourcefileUrl, DiagnosticSeverity severity, String message) {
        this.severity = severity;
        this.message = message;
        this.pos = new Position() {

            @Override
            public int getFirstCol() {
                return 2;
            }

            @Override
            public int getFirstLine() {
                return linenumber;
            }

            @Override
            public int getFirstOffset() {
                return 0;
            }

            @Override
            public int getLastCol() {
                return 10;
            }

            @Override
            public int getLastLine() {
                return linenumber;
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
        return DiagnosticSeverity.Error;
    }

    @Override
    public Pair<CAstSourcePositionMap.Position, String> repair() {
        return Pair.make(pos, "Hardcoded text");
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
