package goblintserver;

public class GobPieConfiguration {

    private String   goblintConf = "";
    private String[] files;
    private String[] preAnalyzeCommand;

    public String getGoblintConf() {
        return this.goblintConf;
    }

    public String[] getFiles() {
        return this.files;
    }

    public String[] getPreAnalyzeCommand() {
        if (preAnalyzeCommand.length < 0) return null;
        return this.preAnalyzeCommand;
    }

}
