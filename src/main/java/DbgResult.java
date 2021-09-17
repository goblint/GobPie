import magpiebridge.core.Kind;

import java.util.ArrayList;

public class DbgResult {

    public String message;
    public int lineStart;
    public int lineEnd;
    public int columnStart;
    public int columnEnd;
    public Kind kind;
    public String fileName;
    public ArrayList<DbgResult> related;
}