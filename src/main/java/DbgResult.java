import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;
import magpiebridge.util.SourceCodePositionFinder;
import magpiebridge.util.SourceCodeReader;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;


public class DbgResult implements AnalysisResult {

    DiagnosticSeverity severity;
    String message;
    final Position pos;


    public DbgResult(int linenumber, URL sourcefileUrl, DiagnosticSeverity severity, String message) {
        this.severity = severity;
        this.message = message;
        // temporarily: display the message on the whole line
        this.pos = SourceCodePositionFinder.findCode(new File(sourcefileUrl.getPath()), linenumber).toPosition();
        // in future: specify the exact columns where the assertion starts and ends
        /*
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
        */
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
