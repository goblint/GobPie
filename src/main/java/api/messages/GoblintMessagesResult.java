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

public class GoblintMessagesResult {

    String type = getClass().getName();

    private final List<tag> tags = new ArrayList<>();
    private String severity;
    private multipiece multipiece;

    public interface tag {

        String toString();

        class Category implements tag {
            private final List<String> Category = new ArrayList<>();

            @Override
            public String toString() {
                return (Category.size() > 0) ? "[" + String.join(" > ", Category) + "]" : "";
            }

        }

        class CWE implements tag {
            private Integer CWE;

            @Override
            public String toString() {
                return (CWE != null) ? "[CWE-" + CWE + "]" : "";
            }
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

    public List<AnalysisResult> convert() {
        List<AnalysisResult> results = new ArrayList<>();

        if (multipiece.group_text == null) {
            GoblintMessagesAnalysisResult result = createGoblintAnalysisResult();
            results.add(result);
        } else {
            List<GoblintMessagesAnalysisResult> intermresults = new ArrayList<>();
            List<multipiece.pieces> pieces = multipiece.pieces;
            for (multipiece.pieces piece : pieces) {
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
                    : new GoblintPosition(
                    multipiece.loc.line,
                    multipiece.loc.endLine,
                    multipiece.loc.column < 0 ? 0 : multipiece.loc.column - 1,
                    multipiece.loc.endColumn < 0 ? 10000 : multipiece.loc.endColumn - 1,
                    new File(multipiece.loc.file).toURI().toURL());
            String msg = tags.stream().map(tag::toString).collect(Collectors.joining("")) + " " + multipiece.text;
            return new GoblintMessagesAnalysisResult(pos, msg, severity);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


    public GoblintMessagesAnalysisResult createGoblintAnalysisResult(multipiece.pieces piece) {
        try {
            GoblintPosition pos = piece.loc == null
                    ? new GoblintPosition(1, 1, 1, new File("").toURI().toURL())
                    : new GoblintPosition(
                    piece.loc.line,
                    piece.loc.endLine,
                    piece.loc.column < 0 ? 0 : piece.loc.column - 1,
                    piece.loc.endColumn < 0 ? 10000 : piece.loc.endColumn - 1,
                    new File(piece.loc.file).toURI().toURL());
            return new GoblintMessagesAnalysisResult(pos,
                    tags.stream().map(tag::toString).collect(Collectors.joining("")) + " Group: " + multipiece.group_text,
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