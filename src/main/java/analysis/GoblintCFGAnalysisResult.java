package analysis;

import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.util.collections.Pair;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.Collections;

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
    private final CAstSourcePositionMap.Position pos;
    private final String funName;
    private final String fileName;
    private final Iterable<Pair<CAstSourcePositionMap.Position, String>> related = new ArrayList<>();

    public GoblintCFGAnalysisResult(CAstSourcePositionMap.Position pos, String funName, String fileName) {
        this.pos = pos;
        this.funName = funName;
        this.fileName = fileName;
    }

    @Override
    public Iterable<Command> command() {
        // TODO: "hardcoded" (currently needs manually executing the --html)
        // TODO: Ask cfg from Goblint with a request instead
        String cfgPath = "http://localhost:8080/cfgs/src%252F" + fileName + "/" + funName + ".svg";
        Command command = new Command("show cfg", "showcfg", Collections.singletonList(cfgPath));
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
    public CAstSourcePositionMap.Position position() {
        return pos;
    }

    @Override
    public Iterable<Pair<CAstSourcePositionMap.Position, String>> related() {
        return related;
    }

    @Override
    public DiagnosticSeverity severity() {
        return DiagnosticSeverity.Information;
    }

    @Override
    public Pair<CAstSourcePositionMap.Position, String> repair() {
        return null;
    }

    @Override
    public String code() {
        return null;
    }
}
