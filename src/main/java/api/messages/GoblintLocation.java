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
public record GoblintLocation(
        String file,
        int line,
        int column,
        Integer endLine,
        Integer endColumn) {

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
