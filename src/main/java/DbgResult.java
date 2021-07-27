import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import java.io.File;
import java.net.URL;
import magpiebridge.core.Kind;
import magpiebridge.util.SourceCodePositionFinder;

public class DbgResult {

    String message;
    int lineStart;
    int lineEnd;
    int columnStart;
    int columnEnd;
    Kind kind;

    public DbgResult(String message, int linenumber, int columnStart, int columnEnd) {
        this.message = message;
        this.lineStart = linenumber;
        this.lineEnd = linenumber;
        this.columnStart = columnStart;
        this.columnEnd = columnEnd;
        this.kind = Kind.Diagnostic;
    }

    public DbgResult(String message, int lineStart, int lineEnd, URL sourcefileUrl) {
        this.message = message;
        this.lineStart = lineStart;
        this.lineEnd = lineEnd;
        this.columnStart = SourceCodePositionFinder.findCode(new File(sourcefileUrl.getPath()), lineStart).toPosition().getFirstCol();
        this.columnEnd = SourceCodePositionFinder.findCode(new File(sourcefileUrl.getPath()), lineEnd).toPosition().getLastCol();
        this.kind = Kind.Hover;
    }

    public DbgResult(String message, int linenumber, URL sourcefileUrl) {
        this.message = message;
        this.lineStart = linenumber;
        this.lineEnd = linenumber;
        Position pos = SourceCodePositionFinder.findCode(new File(sourcefileUrl.getPath()), linenumber).toPosition();
        this.columnStart = pos.getFirstCol();
        this.columnEnd = pos.getLastCol();
        this.kind = Kind.Hover;
    }
}