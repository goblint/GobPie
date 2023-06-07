package api.messages;

import analysis.GoblintMessagesAnalysisResult;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.Pair;
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
    private final List<Tag> tags = new ArrayList<>();
    private String severity;
    private Multipiece multipiece;

    public interface Tag {
        String toString();
    }

    public static class Category implements Tag {
        private final List<String> Category = new ArrayList<>();

        @Override
        public String toString() {
            return (Category.size() > 0) ? "[" + String.join(" > ", Category) + "]" : "";
        }
    }

    public static class CWE implements Tag {
        private Integer CWE;

        @Override
        public String toString() {
            return (CWE != null) ? "[CWE-" + CWE + "]" : "";
        }
    }

    static class Multipiece {
        private GoblintLocation loc;
        private String text;
        private String group_text;
        private final List<Piece> pieces = new ArrayList<>();

        static class Piece {
            private String text;
            private GoblintLocation loc;
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

    public GoblintPosition getLocation(GoblintLocation loc) {
        try {
            return loc == null
                    ? new GoblintPosition(1, 1, 1, new File("").toURI().toURL())
                    : loc.toPosition();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public GoblintPosition getRandomLocation(Multipiece multipiece) {
        for (GoblintMessagesResult.Multipiece.Piece piece : multipiece.pieces) {
            if (piece.loc != null) return getLocation(piece.loc);
        }
        return getLocation(multipiece.loc);
    }

    public GoblintMessagesAnalysisResult createGoblintAnalysisResult() {
        GoblintPosition pos = getLocation(multipiece.loc);
        String msg = tags.stream().map(Tag::toString).collect(Collectors.joining("")) + " " + multipiece.text;
        return new GoblintMessagesAnalysisResult(pos, msg, severity);

    }

    public GoblintMessagesAnalysisResult createGoblintAnalysisResult(Multipiece.Piece piece, boolean addGroupText) {
        GoblintPosition pos = getLocation(piece.loc);
        return new GoblintMessagesAnalysisResult(
                pos,
                addGroupText
                        ? tags.stream().map(Tag::toString).collect(Collectors.joining("")) + " Group: " + multipiece.group_text
                        : "",
                piece.text,
                severity);
    }

    public GoblintMessagesAnalysisResult createGoblintAnalysisResult(Multipiece multipiece, List<Pair<Position, String>> related) {
        GoblintPosition pos = multipiece.loc != null
                ? getLocation(multipiece.loc)
                : getRandomLocation(multipiece);
        return new GoblintMessagesAnalysisResult(
                pos,
                tags.stream().map(Tag::toString).collect(Collectors.joining("")) + " " + this.multipiece.group_text,
                severity,
                related);
    }
}