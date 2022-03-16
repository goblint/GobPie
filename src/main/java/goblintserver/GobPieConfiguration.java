package goblintserver;

public class GobPieConfiguration {

    private String   goblintConf = "";
    private String[] preAnalyzeCommand;

    public String getGoblintConf() {
        return this.goblintConf;
    }

    public String[] getPreAnalyzeCommand() {
        if (preAnalyzeCommand == null || preAnalyzeCommand.length == 0) return null;
        return this.preAnalyzeCommand;
    }

}
