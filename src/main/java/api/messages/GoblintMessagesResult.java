package api.messages;

import analysis.GoblintMessagesAnalysisResult;
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

    public List<AnalysisResult> convertSingle() {
        GoblintMessagesAnalysisResult result = createGoblintAnalysisResult();
        return new ArrayList<>(List.of(result));
    }

    public List<AnalysisResult> convertGroupToSeparateWarnings() {
        List<GoblintMessagesAnalysisResult> resultsWithoutRelated =
                multipiece.pieces.stream().map(this::createGoblintAnalysisResult).toList();
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

    public List<AnalysisResult> convert() {
        if (multipiece.group_text == null) {
            return convertSingle();
        } else {
            return convertGroupToSeparateWarnings();
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

    public GoblintMessagesAnalysisResult createGoblintAnalysisResult() {
        try {
            GoblintPosition pos = multipiece.loc == null
                    ? new GoblintPosition(1, 1, 1, new File("").toURI().toURL())
                    : locationToPosition(multipiece.loc);
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
                    : locationToPosition(piece.loc);
            return new GoblintMessagesAnalysisResult(pos,
                    tags.stream().map(tag::toString).collect(Collectors.joining("")) + " Group: " + multipiece.group_text,
                    piece.text, severity);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}