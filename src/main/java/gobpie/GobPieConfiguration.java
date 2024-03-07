package gobpie;

import java.util.Arrays;
import java.util.Objects;

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

    private GobPieConfiguration(String goblintExecutable, String goblintConf, String[] preAnalyzeCommand, boolean abstractDebugging, boolean showCfg, boolean explodeGroupWarnings, boolean incrementalAnalysis) {
        this.goblintExecutable = goblintExecutable;
        this.goblintConf = goblintConf;
        this.preAnalyzeCommand = preAnalyzeCommand;
        this.abstractDebugging = abstractDebugging;
        this.showCfg = showCfg;
        this.explodeGroupWarnings = explodeGroupWarnings;
        this.incrementalAnalysis = incrementalAnalysis;
    }

    public String getGoblintExecutable() {
        return this.goblintExecutable;
    }

    public String getGoblintConf() {
        return this.goblintConf;
    }

    public String[] getPreAnalyzeCommand() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GobPieConfiguration that = (GobPieConfiguration) o;
        return abstractDebugging == that.abstractDebugging && showCfg == that.showCfg && explodeGroupWarnings == that.explodeGroupWarnings && incrementalAnalysis == that.incrementalAnalysis && Objects.equals(goblintExecutable, that.goblintExecutable) && Objects.equals(goblintConf, that.goblintConf) && Arrays.equals(preAnalyzeCommand, that.preAnalyzeCommand);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(goblintExecutable, goblintConf, abstractDebugging, showCfg, explodeGroupWarnings, incrementalAnalysis);
        result = 31 * result + Arrays.hashCode(preAnalyzeCommand);
        return result;
    }

    @Override
    public String toString() {
        return "GobPieConfiguration{" +
                "goblintExecutable='" + goblintExecutable + '\'' +
                ", goblintConf='" + goblintConf + '\'' +
                ", preAnalyzeCommand=" + Arrays.toString(preAnalyzeCommand) +
                ", abstractDebugging=" + abstractDebugging +
                ", showCfg=" + showCfg +
                ", explodeGroupWarnings=" + explodeGroupWarnings +
                ", incrementalAnalysis=" + incrementalAnalysis +
                '}';
    }

    public static class Builder {
        private String goblintExecutable;
        private String goblintConf;
        private String[] preAnalyzeCommand;
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

        public Builder setPreAnalyzeCommand(String[] preAnalyzeCommand) {
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
