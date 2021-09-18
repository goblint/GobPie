import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.Pair;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import magpiebridge.util.SourceCodeInfo;
import magpiebridge.util.SourceCodePositionFinder;


public class GoblintResult {

    private String severity;
    private multipiece multipiece;
    URL sourcefileURL;

    static class multipiece {

        private loc loc;
        private String text;
        private String group_text;
        private List<pieces> pieces = new ArrayList<pieces>();

        static class loc {

            private int line;
            private int column;

        }

        static class pieces {

            private String text;
            private loc loc;

            static class loc {

                private int line;
                private int column;
            }
        }

    }

    public List<GoblintAnalysisResult> convert() {
        List<GoblintAnalysisResult> results = new ArrayList<>();

        if (multipiece.group_text == null) {
            String message = multipiece.text;
            GoblintPosition pos = new GoblintPosition(
                multipiece.loc.line,
                multipiece.loc.column - 1,
                findColumnEnd(multipiece.loc.line, multipiece.loc.column), 
                sourcefileURL);
                GoblintAnalysisResult result = new GoblintAnalysisResult(pos, message, severity);
                results.add(result);
        } else {
            List<GoblintAnalysisResult> intermresults = new ArrayList<>();
            List<multipiece.pieces> pieces = multipiece.pieces;
            for (multipiece.pieces piece : pieces) {
                String message = multipiece.group_text + ", " + piece.text;
                GoblintPosition pos = new GoblintPosition(
                piece.loc.line,
                piece.loc.column - 1,
                findColumnEnd(piece.loc.line, piece.loc.column), 
                sourcefileURL);
                GoblintAnalysisResult result = new GoblintAnalysisResult(pos, message, severity);
                intermresults.add(result);
            }
            // Add related warnings to all the group elements
            List<GoblintAnalysisResult> addedRelated = new ArrayList<>();
            for (GoblintAnalysisResult res1 : intermresults) {
                List<Pair<Position, String>> related = new ArrayList<>();
                for (GoblintAnalysisResult res2 : intermresults) {
                    if (res1 != res2) {
                        related.add(Pair.make(res2.position(), res2.toString()));
                    }
                }
                addedRelated.add(new GoblintAnalysisResult(res1.position(), res1.message(), res1.severityStr(), related));
            }
            results.addAll(addedRelated);
        }
        return results;      
    }

    public int findColumnEnd(int lineStart, int columnStart) {

        // get source code of the specified line
        SourceCodeInfo sourceCodeInfo = SourceCodePositionFinder.findCode(new File(sourcefileURL.getPath()), lineStart);
        // get the source code substring starting from the relevant assert statement.
        // as the source code is given without the leading whitespace, but the column numbers take whitespace into account
        // the offset must be subtracted from the original starting column which does include the leading whitespace
        String sourceCode = sourceCodeInfo.code.substring(columnStart - sourceCodeInfo.range.getStart().getCharacter());
        // find the index of the next semicolon
        int indexOfNextSemicolon = sourceCode.indexOf(";") + 1;

        return columnStart + indexOfNextSemicolon;
    }
    

}
