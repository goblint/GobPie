package analysis;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.Pair;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

/**
 * The Class GoblintCFGAnalysisResult.
 * <p>
 * Implementation of the GoblintAnalysisResult class that extends MagpieBridge AnalysisResult class.
 * The class that corresponds to the CFG code lenses.
 *
 * @author Karoliine Holter
 * @since 0.0.3
 */

public class GoblintCFGAnalysisResult implements AnalysisResult {
    private final Position pos;
    private final String title;
    private final String funName;
    private final Iterable<Pair<Position, String>> related = new ArrayList<>();

    public GoblintCFGAnalysisResult(Position pos, String title, String funName) {
        this.pos = pos;
        this.title = title;
        this.funName = funName;
    }

    @Override
    public Iterable<Command> command() {
        Command command = new Command(title, "showcfg", Collections.singletonList(funName));
        return Collections.singleton(command);
    }

    @Override
    public Kind kind() {
        return Kind.CodeLens;
    }

    @Override
    public String toString(boolean useMarkdown) {
        return "cfg";
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
        return DiagnosticSeverity.Information;
    }

    @Override
    public Pair<Position, String> repair() {
        return null;
    }

    @Override
    public String code() {
        return null;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoblintCFGAnalysisResult that = (GoblintCFGAnalysisResult) o;
        return Objects.equals(pos, that.pos) && Objects.equals(title, that.title) && Objects.equals(funName, that.funName) && Objects.equals(related, that.related);
    }

}
