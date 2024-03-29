package api.messages;

import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.net.MalformedURLException;

/**
 * Goblint CIL location.
 *
 * @author Juhan Oskar Hennoste
 * @since 0.0.4
 */
public class GoblintLocation {

    private String file;
    private int line;
    private int column;
    private Integer endLine;
    private Integer endColumn;

    // Byte offsets from start of file.
    // This duplicates information from line and column fields, but needs to exist when using the location as a parameter.
    @SerializedName("byte")
    private int startByte = -1;

    public GoblintLocation(String file, int line, int column) {
        this.file = file;
        this.line = line;
        this.column = column;
    }

    public GoblintLocation(String file, int line, int column, int endLine, int endColumn) {
        this.file = file;
        this.line = line;
        this.column = column;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public Integer getEndColumn() {
        return endColumn;
    }

    public GoblintPosition toPosition() {
        try {
            return new GoblintPosition(
                    this.line,
                    this.endLine,
                    this.column < 0 ? 0 : this.column - 1,
                    this.endColumn < 0 ? 10000 : this.endColumn - 1,
                    new File(this.file).toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return file + " " +
                line + ":" + column +
                (endLine == null && endColumn == null ? "" : "-" + endLine + ":" + endColumn);
    }
}
