package gobpie;

import java.util.List;

/**
 * The Class GobPieConfiguration.
 * <p>
 * Corresponding object to the GobPie configuration JSON.
 *
 * @author Karoliine Holter
 * @since 0.0.2
 */
public record GobPieConfiguration(
        String goblintExecutable,
        String goblintConf,
        List<String> preAnalyzeCommand,
        Boolean abstractDebugging,
        Boolean showCfg,
        Boolean explodeGroupWarnings,
        Boolean incrementalAnalysis) {

    public GobPieConfiguration(String goblintExecutable, String goblintConf, List<String> preAnalyzeCommand, Boolean abstractDebugging, Boolean showCfg, Boolean explodeGroupWarnings, Boolean incrementalAnalysis) {
        this.goblintExecutable = (goblintExecutable == null) ? "goblint" : goblintExecutable;
        this.goblintConf = goblintConf;
        this.preAnalyzeCommand = preAnalyzeCommand;
        this.abstractDebugging = abstractDebugging != null && abstractDebugging;
        this.showCfg = showCfg != null && showCfg;
        this.explodeGroupWarnings = explodeGroupWarnings == null || explodeGroupWarnings;
        this.incrementalAnalysis = incrementalAnalysis == null || incrementalAnalysis;
    }

    public String getGoblintExecutable() {
        return this.goblintExecutable;
    }

    public String getGoblintConf() {
        return this.goblintConf;
    }

    public String[] getPreAnalyzeCommand() {
        return this.preAnalyzeCommand.toArray(new String[0]);
    }

    public boolean enableAbstractDebugging() {
        return abstractDebugging;
    }

    public Boolean showCfg() {
        return this.showCfg;
    }

    public Boolean explodeGroupWarnings() {
        return explodeGroupWarnings;
    }

    public boolean useIncrementalAnalysis() {
        return incrementalAnalysis;
    }

    public static class Builder {
        private String goblintExecutable;
        private String goblintConf;
        private List<String> preAnalyzeCommand;
        private boolean abstractDebugging;
        private boolean showCfg;
        private boolean explodeGroupWarnings;
        private boolean incrementalAnalysis;

        public Builder setGoblintExecutable(String goblintExecutable) {
            this.goblintExecutable = goblintExecutable;
            return this;
        }

        public Builder setGoblintConf(String goblintConf) {
            this.goblintConf = goblintConf;
            return this;
        }

        public Builder setPreAnalyzeCommand(List<String> preAnalyzeCommand) {
            this.preAnalyzeCommand = preAnalyzeCommand;
            return this;
        }

        public Builder setAbstractDebugging(boolean abstractDebugging) {
            this.abstractDebugging = abstractDebugging;
            return this;
        }

        public Builder setShowCfg(boolean showCfg) {
            this.showCfg = showCfg;
            return this;
        }

        public Builder setExplodeGroupWarnings(boolean explodeGroupWarnings) {
            this.explodeGroupWarnings = explodeGroupWarnings;
            return this;
        }

        public Builder setIncrementalAnalysis(boolean incrementalAnalysis) {
            this.incrementalAnalysis = incrementalAnalysis;
            return this;
        }

        public GobPieConfiguration createGobPieConfiguration() {
            return new GobPieConfiguration(goblintExecutable, goblintConf, preAnalyzeCommand, abstractDebugging, showCfg, explodeGroupWarnings, incrementalAnalysis);
        }
    }
}
