package analysis;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IMethod;

import java.io.Reader;
import java.net.URL;

public class GoblintPosition implements Position {

    private int columnStart;
    private int columnEnd;
    private int lineStart;
    private int lineEnd;
    private URL sourcefileURL;

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
