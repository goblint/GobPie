import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.Pair;

import java.util.ArrayList;

import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;
import magpiebridge.util.SourceCodeReader;

import org.eclipse.lsp4j.DiagnosticSeverity;

public class GoblintAnalysisResult implements AnalysisResult {

    private String message;
    private Position pos;
    private String severity;
    private Iterable<Pair<Position, String>> related = new ArrayList<>();

    public GoblintAnalysisResult(GoblintPosition pos, String message, String severity) {
        this.message = message;
        this.pos = pos;
        this.severity = severity;
    }

    public GoblintAnalysisResult(Position pos, String message, String severity, Iterable<Pair<Position, String>> related) {
        this.message = message;
        this.pos = pos;
        this.severity = severity;
        this.related = related;
    }

    @Override
    public Kind kind() {
        return Kind.Diagnostic;
    }

    @Override
    public String toString(boolean useMarkdown) {
        return message;
    }

    public String message() {
        return message;
    }

    @Override
    public Position position() {
        return pos;
    }

    @Override
    public Iterable<Pair<Position, String>> related() {
        return related;
    }

    @Override
    public DiagnosticSeverity severity() {
        DiagnosticSeverity diagnosticSeverity = DiagnosticSeverity.Information;
        if (severity.equals("Warning")) {
            diagnosticSeverity = DiagnosticSeverity.Warning;
        } else if (severity.equals("Error")) {
            diagnosticSeverity = DiagnosticSeverity.Error;
        }
        return diagnosticSeverity;
    }

    public String severityStr() {
        return severity;
    }

    @Override
    public Pair<Position, String> repair() {
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
