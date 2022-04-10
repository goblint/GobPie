package analysis;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.Pair;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Class GoblintMessages.
 * 
 * Corresponding object to the Goblint results in JSON.
 * Converts the results from JSON to AnalysisResult requested by MagpieBridge.
 * 
 * @author      Karoliine Holter
 * @since       0.0.1
 */

public class GoblintMessages {

        private List<tag> tags = new ArrayList<>();
        private String severity;
        private multipiece multipiece;

        static interface tag {

            public String toString();

            static class Category implements tag {
                private List<String> Category = new ArrayList<String>();

                @Override
                public String toString() {
                    return (Category.size() > 0) ? "[" + String.join(" > ", Category) + "]" : "";
                }

            }

            static class CWE implements tag {
                private Integer CWE;

                @Override
                public String toString() {
                    return (CWE != null) ? "[CWE-" + CWE + "]" : "";
                }
            }
        }

        static class multipiece {

            private loc loc;
            private String text;
            private String group_text;
            private List<pieces> pieces = new ArrayList<pieces>();

            static class loc {

                private String file;
                private int line;
                private int column;
                private int endLine;
                private int endColumn;

            }

            static class pieces {

                private String text;
                private loc loc;

                static class loc {

                    private String file;
                    private int line;
                    private int column;
                    private int endLine;
                    private int endColumn;
                }
            }
    }


    public List<GoblintAnalysisResult> convert() throws MalformedURLException {
        List<GoblintAnalysisResult> results = new ArrayList<>();

            if (multipiece.group_text == null) {
                GoblintAnalysisResult result = createGoblintAnalysisResult();
                results.add(result);
            } else {
                List<GoblintAnalysisResult> intermresults = new ArrayList<>();
                List<multipiece.pieces> pieces = multipiece.pieces;
                for (multipiece.pieces piece : pieces) {
                    GoblintAnalysisResult result = createGoblintAnalysisResult(piece);
                    intermresults.add(result);
                }
                // Add related warnings to all the group elements
                List<GoblintAnalysisResult> addedRelated = new ArrayList<>();
                for (GoblintAnalysisResult res1 : intermresults) {
                    List<Pair<Position, String>> related = new ArrayList<>();
                    for (GoblintAnalysisResult res2 : intermresults) {
                        if (res1 != res2) {
                            related.add(Pair.make(res2.position(), res2.text()));
                        }
                    }
                    addedRelated.add(new GoblintAnalysisResult(res1.position(), res1.group_text() + "\n" + res1.text(),
                            res1.severityStr(), related));
                }
                results.addAll(addedRelated);
        }

        return results;
    }


    public GoblintAnalysisResult createGoblintAnalysisResult() throws MalformedURLException {
        int column = multipiece.loc.column < 0 ? 1 : multipiece.loc.column - 1;
        int endColumn = multipiece.loc.endColumn< 0 ? 10000 : multipiece.loc.endColumn - 1;
        GoblintPosition pos = multipiece.loc == null 
                              ? new GoblintPosition(1, 1, 1, new File("").toURI().toURL()) 
                              : new GoblintPosition(multipiece.loc.line, multipiece.loc.endLine, column, endColumn, new File(multipiece.loc.file).toURI().toURL());
        String msg = tags.stream().map(tag -> tag.toString()).collect(Collectors.joining("")) + " " + multipiece.text;
        return new GoblintAnalysisResult(pos, msg, severity);
    }


    public GoblintAnalysisResult createGoblintAnalysisResult(multipiece.pieces piece) throws MalformedURLException {
        int column = piece.loc.column < 0 ? 1 : piece.loc.column - 1;
        int endColumn = piece.loc.endColumn< 0 ? 10000 : piece.loc.endColumn - 1;
        GoblintPosition pos = piece.loc == null
                              ? new GoblintPosition(1, 1, 1, new File("").toURI().toURL())
                              : new GoblintPosition(piece.loc.line, piece.loc.endLine, column, endColumn, new File(piece.loc.file).toURI().toURL());
        return new GoblintAnalysisResult(pos,
                    tags.stream().map(tag -> tag.toString()).collect(Collectors.joining("")) + " Group: " + multipiece.group_text,
                    piece.text, severity);
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
