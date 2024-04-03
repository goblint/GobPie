package analysis;

import api.messages.GoblintPosition;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.Pair;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.Objects;

/**
 * The Class GoblintMessagesAnalysisResult.
 * <p>
 * Implementation of the GoblintAnalysisResult class that extends MagpieBridge AnalysisResult class.
 * The class that corresponds to the Goblint warnings that are shown in the IDE.
 *
 * @author Karoliine Holter
 */

public class GoblintMessagesAnalysisResult implements AnalysisResult {

    private String group_text = "";
    private final String text;
    private final Position pos;
    private final String severity;
    private Iterable<Pair<Position, String>> related = new ArrayList<>();

    public GoblintMessagesAnalysisResult(GoblintPosition pos, String text, String severity) {
        this.text = text;
        this.pos = pos;
        this.severity = severity;
    }

    public GoblintMessagesAnalysisResult(GoblintPosition pos, String group_text, String text, String severity) {
        this.group_text = group_text;
        this.text = text;
        this.pos = pos;
        this.severity = severity;
    }


    public GoblintMessagesAnalysisResult(Position pos, String text, String severity, Iterable<Pair<Position, String>> related) {
        this.text = text;
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
        return text;
    }

    public String group_text() {
        return group_text;
    }

    public String text() {
        return text;
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
        // String code;
        // try {
        //     code = SourceCodeReader.getLinesInString(pos);
        // } catch (Exception e) {
        //     throw new RuntimeException(e);
        // }
        // return code;
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoblintMessagesAnalysisResult that = (GoblintMessagesAnalysisResult) o;
        return Objects.equals(group_text, that.group_text) && Objects.equals(text, that.text) && Objects.equals(pos, that.pos) && Objects.equals(severity, that.severity) && Objects.equals(related, that.related);
    }

}
