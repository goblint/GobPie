package analysis;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import magpiebridge.command.CodeActionCommand;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;
// import magpiebridge.util.SourceCodeReader;

import magpiebridge.util.SourceCodePositionUtils;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import goblintclient.messages.GoblintPosition;

/**
 * The Class GoblintAnalysisResult.
 * <p>
 * Implementation of the MagpieBridge AnalysisResult class.
 * Customizes it for the needs of Goblint.
 *
 * @author Julian Dolby, Linghui Luo and Karoliine Holter
 */

public class GoblintAnalysisResult implements AnalysisResult {

    private String group_text = "";
    private final String text;
    private final Position pos;
    private final String severity;
    private Iterable<Pair<Position, String>> related = new ArrayList<>();

    public GoblintAnalysisResult(GoblintPosition pos, String text, String severity) {
        this.text = text;
        this.pos = pos;
        this.severity = severity;
    }

    public GoblintAnalysisResult(GoblintPosition pos, String group_text, String text, String severity) {
        this.group_text = group_text;
        this.text = text;
        this.pos = pos;
        this.severity = severity;
    }


    public GoblintAnalysisResult(Position pos, String text, String severity, Iterable<Pair<Position, String>> related) {
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
    public Iterable<Command> command() {
        // TODO: hardcoded
        String cfgPath = "http://localhost:8080/cfgs/src%252Fexample.c/main.svg";
        Command command = new Command("show cfg", "showcfg", Collections.singletonList(cfgPath));
        return Collections.singleton(command);
    }

}
