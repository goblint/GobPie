package gobpie;

/**
 * The Class GobPieConfiguration.
 * <p>
 * Corresponding object to the GobPie configuration JSON.
 *
 * @author Karoliine Holter
 * @since 0.0.2
 */
@SuppressWarnings({"FieldCanBeLocal"})
public class GobPieConfiguration {

    private final String goblintExecutable = "goblint";
    private String goblintConf;
    private String[] preAnalyzeCommand;
    private final boolean showCfg = false;
    private final boolean incrementalAnalysis = true;
    private final boolean explodeGroupWarnings = true;

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

    public boolean showCfg() {
        return this.showCfg;
    }

    public boolean useIncrementalAnalysis() {
        return incrementalAnalysis;
    }

    public boolean explodeGroupWarnings() {
        return explodeGroupWarnings;
    }

}
