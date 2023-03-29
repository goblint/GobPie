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
    private final List<tag> tags = new ArrayList<>();
    private String severity;
    private Multipiece multipiece;

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

    static class multipiece {
        private GoblintLocation loc;
        private String text;
        private String group_text;
        private final List<Piece> pieces = new ArrayList<>();

        static class pieces {
            private String text;
            private GoblintLocation loc;
        }
    }

    public String getType() {
        return type;
    }

    public List<AnalysisResult> convert() {
        List<AnalysisResult> results = new ArrayList<>();

        if (multipiece.group_text == null) {
            GoblintMessagesAnalysisResult result = createGoblintAnalysisResult();
            results.add(result);
        } else {
            List<GoblintMessagesAnalysisResult> intermresults = new ArrayList<>();
            List<Multipiece.Piece> pieces = multipiece.pieces;
            for (Multipiece.Piece piece : pieces) {
                GoblintMessagesAnalysisResult result = createGoblintAnalysisResult(piece);
                intermresults.add(result);
            }
            // Add related warnings to all the group elements
            List<GoblintMessagesAnalysisResult> addedRelated = new ArrayList<>();
            for (GoblintMessagesAnalysisResult res1 : intermresults) {
                List<Pair<Position, String>> related = new ArrayList<>();
                for (GoblintMessagesAnalysisResult res2 : intermresults) {
                    if (res1 != res2) {
                        related.add(Pair.make(res2.position(), res2.text()));
                    }
                }
                addedRelated.add(new GoblintMessagesAnalysisResult(res1.position(), res1.group_text() + "\n" + res1.text(),
                        res1.severityStr(), related));
            }
            results.addAll(addedRelated);
        }

        return results;
    }

    public GoblintMessagesAnalysisResult createGoblintAnalysisResult() {
        try {
            GoblintPosition pos = multipiece.loc == null
                    ? new GoblintPosition(1, 1, 1, new File("").toURI().toURL())
                    : multipiece.loc.toPosition();
            String msg = tags.stream().map(Tag::toString).collect(Collectors.joining("")) + " " + multipiece.text;
            return new GoblintMessagesAnalysisResult(pos, msg, severity);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


    public GoblintMessagesAnalysisResult createGoblintAnalysisResult(Multipiece.Piece piece) {
        try {
            GoblintPosition pos = piece.loc == null
                    ? new GoblintPosition(1, 1, 1, new File("").toURI().toURL())
                    : piece.loc.toPosition();
            return new GoblintMessagesAnalysisResult(pos,
                    tags.stream().map(Tag::toString).collect(Collectors.joining("")) + " Group: " + multipiece.group_text,
                    piece.text, severity);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


    // public List<String> getFiles() {
    //     Set<String> allFiles = new HashSet<>();
    //     for (Entry<String, List<String>> entry : files.entrySet()) {
    //         allFiles.add(entry.getKey());
    //         allFiles.addAll(entry.getValue());
    //     }
    //     return new ArrayList<>(allFiles);
    // }

}