package gobpie;

/**
 * The Class GobPieConfiguration.
 * <p>
 * Corresponding object to the GobPie configuration JSON.
 *
 * @author Karoliine Holter
 * @since 0.0.2
 */
public class GobPieConfiguration {

    private final String goblintExecutable;
    private String goblintConf;
    private String[] preAnalyzeCommand;
    private final boolean abstractDebugging;
    private final boolean showCfg;
    private final boolean explodeGroupWarnings;
    private final boolean incrementalAnalysis;

    private GobPieConfiguration() {
        goblintExecutable = "goblint";
        abstractDebugging = false;
        showCfg = false;
        explodeGroupWarnings = true;
        incrementalAnalysis = true;
    }

    public String getGoblintExecutable() {
        return this.goblintExecutable;
    }

    public String getGoblintConf() {
        return this.goblintConf;
    }

    public String[] getPreAnalyzeCommand() {
        if (preAnalyzeCommand == null || preAnalyzeCommand.length == 0) return null;
        return this.preAnalyzeCommand;
    }

    public boolean enableAbstractDebugging() {
        return abstractDebugging;
    }

    public boolean showCfg() {
        return this.showCfg;
    }

    public boolean explodeGroupWarnings() {
        return explodeGroupWarnings;
    }

    public boolean useIncrementalAnalysis() {
        return incrementalAnalysis;
    }

}
