public class DbgResult {

    String message;
    int linenumber;
    int columnStart;
    int columnEnd;

    public DbgResult(String message, int linenumber, int columnStart, int columnEnd) {
        this.message = message;
        this.linenumber = linenumber;
        this.columnStart = columnStart;
        this.columnEnd = columnEnd;
    }
}