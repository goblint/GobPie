package analysis;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.Pair;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


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
                String msg = tags.stream().map(tag -> tag.toString()).collect(Collectors.joining("")) + " " + multipiece.text;
                GoblintPosition pos = new GoblintPosition(multipiece.loc.line, multipiece.loc.endLine, multipiece.loc.column - 1, multipiece.loc.endColumn - 1, new File(multipiece.loc.file).toURI().toURL());
                GoblintAnalysisResult result = new GoblintAnalysisResult(pos, msg, severity);
                results.add(result);
            } else {
                List<GoblintAnalysisResult> intermresults = new ArrayList<>();
                List<multipiece.pieces> pieces = multipiece.pieces;
                for (multipiece.pieces piece : pieces) {
                    GoblintPosition pos = new GoblintPosition(piece.loc.line, piece.loc.endLine, piece.loc.column - 1, piece.loc.endColumn - 1, new File(piece.loc.file).toURI().toURL());
                    GoblintAnalysisResult result = new GoblintAnalysisResult(pos,
                            tags.stream().map(tag -> tag.toString()).collect(Collectors.joining("")) + " Group: " + multipiece.group_text,
                            piece.text, severity);
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


    // public List<String> getFiles() {
    //     Set<String> allFiles = new HashSet<>();
    //     for (Entry<String, List<String>> entry : files.entrySet()) {
    //         allFiles.add(entry.getKey());
    //         allFiles.addAll(entry.getValue());
    //     }
    //     return new ArrayList<>(allFiles);
    // }

}
