package goblintserver;

/**
 * The Class GobPieConfiguration.
 * 
 * Corresponding object to the GobPie configuration JSON.
 * 
 * @author      Karoliine Holter
 * @since       0.0.2
 */

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
