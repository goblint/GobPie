package gobpie;

/**
 * The Class GobPieConfiguration.
 * <p>
 * Corresponding object to the GobPie configuration JSON.
 *
 * @author Karoliine Holter
 * @since 0.0.2
 */
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class GobPieConfiguration {

    private String goblintExecutable = "goblint";
    private String goblintConf;
    private String[] preAnalyzeCommand;
    private boolean showCfg = false;
    private boolean incrementalAnalysis = true;
    private boolean explodeGroupWarnings = true;

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
