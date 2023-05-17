package api.messages;

import analysis.GoblintMessagesAnalysisResult;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import magpiebridge.core.AnalysisResult;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Class GoblintMessagesResult.
 * <p>
 * Corresponding object to the Goblint messages request response results in JSON.
 * Converts the results from JSON to AnalysisResult requested by MagpieBridge.
 *
 * @author Karoliine Holter
 * @since 0.0.1
 */

@SuppressWarnings({"unused"})
public class GoblintMessagesResult {

    private final String type = getClass().getName();
    private final List<tag> tags = new ArrayList<>();
    private String severity;
    private multipiece multipiece;

    public interface tag {
        String toString();
    }

    public static class Category implements tag {
        private final List<String> Category = new ArrayList<>();

        @Override
        public String toString() {
            return (Category.size() > 0) ? "[" + String.join(" > ", Category) + "]" : "";
        }
    }

    public static class CWE implements tag {
        private Integer CWE;

        @Override
        public String toString() {
            return (CWE != null) ? "[CWE-" + CWE + "]" : "";
        }
    }

    static class loc {
        private String file;
        private int line;
        private int column;
        private int endLine;
        private int endColumn;
    }

    static class multipiece {
        private loc loc;
        private String text;
        private String group_text;
        private final List<pieces> pieces = new ArrayList<>();

        static class pieces {
            private String text;
            private loc loc;
        }
    }

    public String getType() {
        return type;
    }

    public List<AnalysisResult> convertSingle() {
        GoblintMessagesAnalysisResult result = createGoblintAnalysisResult();
        return new ArrayList<>(List.of(result));
    }

    public List<AnalysisResult> convertGroupToSeparateWarnings() {
        List<GoblintMessagesAnalysisResult> resultsWithoutRelated =
                multipiece.pieces.stream().map(piece -> createGoblintAnalysisResult(piece, true)).toList();
        // Add related warnings to all the pieces in the group
        List<GoblintMessagesAnalysisResult> resultsWithRelated = new ArrayList<>();
        for (GoblintMessagesAnalysisResult result : resultsWithoutRelated) {
            resultsWithRelated.add(
                    new GoblintMessagesAnalysisResult(
                            result.position(),
                            result.group_text() + "\n" + result.text(),
                            result.severityStr(),
                            resultsWithoutRelated.stream()
                                    .filter(res -> res != result)
                                    .map(res -> Pair.make(res.position(), res.text()))
                                    .toList()));
        }
        return new ArrayList<>(resultsWithRelated);
    }

    public List<AnalysisResult> convertGroup() {
        List<Pair<Position, String>> relatedFromPieces =
                multipiece.pieces.stream()
                        .map(piece -> createGoblintAnalysisResult(piece, false))
                        .map(result -> Pair.make(result.position(), result.text()))
                        .toList();
        GoblintMessagesAnalysisResult result = createGoblintAnalysisResult(multipiece, relatedFromPieces);
        return new ArrayList<>(List.of(result));
    }

    public List<AnalysisResult> convertExplode() {
        if (multipiece.group_text == null) {
            return convertSingle();
        } else {
            return convertGroupToSeparateWarnings();
        }
    }

    public List<AnalysisResult> convertNonExplode() {
        if (multipiece.group_text == null) {
            return convertSingle();
        } else {
            return convertGroup();
        }
    }

    public GoblintPosition locationToPosition(loc loc) {
        try {
            return new GoblintPosition(
                    loc.line,
                    loc.endLine,
                    loc.column < 0 ? 0 : loc.column - 1,
                    loc.endColumn < 0 ? 10000 : loc.endColumn - 1,
                    new File(loc.file).toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public GoblintPosition getLocation(loc loc) {
        try {
            return loc == null
                    ? new GoblintPosition(1, 1, 1, new File("").toURI().toURL())
                    : locationToPosition(loc);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public GoblintPosition getRandomLocation(multipiece multipiece) {
        for (GoblintMessagesResult.multipiece.pieces piece : multipiece.pieces) {
            if (piece.loc != null) return getLocation(piece.loc);
        }
        return getLocation(multipiece.loc);
    }

    public GoblintMessagesAnalysisResult createGoblintAnalysisResult() {
        GoblintPosition pos = getLocation(multipiece.loc);
        String msg = tags.stream().map(tag::toString).collect(Collectors.joining("")) + " " + multipiece.text;
        return new GoblintMessagesAnalysisResult(pos, msg, severity);

    }

    public GoblintMessagesAnalysisResult createGoblintAnalysisResult(multipiece.pieces piece, boolean addGroupText) {
        GoblintPosition pos = getLocation(piece.loc);
        return new GoblintMessagesAnalysisResult(
                pos,
                addGroupText
                        ? tags.stream().map(tag::toString).collect(Collectors.joining("")) + " Group: " + multipiece.group_text
                        : "",
                piece.text,
                severity);
    }

    public GoblintMessagesAnalysisResult createGoblintAnalysisResult(multipiece multipiece, List<Pair<Position, String>> related) {
        GoblintPosition pos = multipiece.loc != null
                ? getLocation(multipiece.loc)
                : getRandomLocation(multipiece);
        return new GoblintMessagesAnalysisResult(
                pos,
                tags.stream().map(tag::toString).collect(Collectors.joining("")) + " " + this.multipiece.group_text,
                severity,
                related);
    }
}