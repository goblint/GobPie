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
    private MultiPiece multipiece;

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

    public interface MultiPiece {
        List<AnalysisResult> convert(List<Tag> tags, String severity, boolean explode);
    }

    public static class Piece implements MultiPiece {
        private String text;
        private GoblintLocation loc;
        private context context;

        public static class context {
            private Integer tag;
        }

        /**
         * Converts the Single (Piece type of) Goblint messages from the
         * GoblintMessagesResult type to AnalysisResult that are needed for MagPieBridge.
         *
         * @param tags     the tags of the warning given by Goblint
         * @param severity the severity of the warning given by Goblint
         * @param explode  is not used here and is only used for group warnings
         * @return A collection of AnalysisResult objects.
         */
        public List<AnalysisResult> convert(List<Tag> tags, String severity, boolean explode) {
            GoblintPosition pos = getLocation(loc);
            String ctx = context == null || context.tag == null ? "" : " in context " + context.tag;
            String msg = joinTags(tags) + " " + text + ctx;
            GoblintMessagesAnalysisResult result = new GoblintMessagesAnalysisResult(pos, msg, severity);
            return List.of(result);
        }
    }

    public static class Group implements MultiPiece {
        private String group_text;
        private GoblintLocation group_loc;
        private final List<Piece> pieces = new ArrayList<>();

        /**
         * Converts the Group Goblint messages from the
         * GoblintMessagesResult type to AnalysisResult that are needed for MagPieBridge.
         *
         * @param tags     the tags of the warning given by Goblint
         * @param severity the severity of the warning given by Goblint
         * @param explode  the group warnings are exploded to have one IDE warning for each piece in the group if true,
         *                 if false, only one warning per one Goblint warning is shown in the IDE
         * @return A collection of AnalysisResult objects.
         */
        public List<AnalysisResult> convert(List<Tag> tags, String severity, boolean explode) {
            return explode && this.group_loc != null
                    ? convertGroupExplode(tags, severity)
                    : convertGroup(tags, severity);
        }

        public List<AnalysisResult> convertGroupExplode(List<Tag> tags, String severity) {
            String groupText = joinTags(tags) + " Group: " + group_text;
            List<GoblintMessagesAnalysisResult> resultsWithoutRelated =
                    pieces.stream().map(piece -> new GoblintMessagesAnalysisResult(getLocation(piece.loc), groupText, piece.text, severity)).toList();
            // Add related warnings to all the pieces in the group
            List<AnalysisResult> resultsWithRelated = new ArrayList<>();
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
            return resultsWithRelated;
        }

        public List<AnalysisResult> convertGroup(List<Tag> tags, String severity) {
            // Convert all pieces to pairs of position and text to be used as related warnings
            List<Pair<Position, String>> relatedFromPieces =
                    pieces.stream().map(piece -> Pair.make((Position) getLocation(piece.loc), piece.text)).toList();
            // Use the group location for the warning if defined, or a random one from one of the pieces otherwise
            GoblintPosition pos =
                    group_loc != null
                            ? group_loc.toPosition()
                            : pieces.stream()
                            .filter(piece -> piece.loc != null)
                            .findFirst()
                            .map(piece -> piece.loc.toPosition())
                            .orElse(getLocation(group_loc));
            GoblintMessagesAnalysisResult result =
                    new GoblintMessagesAnalysisResult(pos, joinTags(tags) + " " + group_text, severity, relatedFromPieces);
            return List.of(result);
        }
    }

    private static String joinTags(List<Tag> tags) {
        return tags.stream().map(Tag::toString).collect(Collectors.joining(""));
    }

    private static GoblintPosition getLocation(GoblintLocation loc) {
        try {
            return loc == null
                    ? new GoblintPosition(1, 1, 1, new File("").toURI().toURL())
                    : loc.toPosition();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AnalysisResult> convert(boolean explode) {
        return multipiece.convert(tags, severity, explode);
    }

}