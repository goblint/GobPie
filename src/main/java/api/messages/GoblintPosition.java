package api.messages;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IMethod;

import java.io.Reader;
import java.net.URL;

/**
 * The Class GoblintPosition.
 *
 * @author Karoliine Holter
 * @since 0.0.1
 */

public class GoblintPosition implements Position {

    private final int columnStart;
    private final int columnEnd;
    private final int lineStart;
    private final int lineEnd;
    private final URL sourcefileURL;

    public GoblintPosition(int lineStart, int lineEnd, int columnStart, int columnEnd, URL sourcefileURL) {
        this.lineStart = lineStart;
        this.lineEnd = lineEnd;
        this.columnStart = columnStart;
        this.columnEnd = columnEnd;
        this.sourcefileURL = sourcefileURL;
    }

    public GoblintPosition(int lineStart, int columnStart, int columnEnd, URL sourcefileURL) {
        this.lineStart = lineStart;
        this.lineEnd = lineStart;
        this.columnStart = columnStart;
        this.columnEnd = columnEnd;
        this.sourcefileURL = sourcefileURL;
    }

    @Override
    public int getFirstCol() {
        return columnStart;
    }

    @Override
    public int getFirstLine() {
        return lineStart;
    }

    @Override
    public int getFirstOffset() {
        return 0;
    }

    @Override
    public int getLastCol() {
        return columnEnd;
    }

    @Override
    public int getLastLine() {
        return lineEnd;
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
        return sourcefileURL;
    }
}
