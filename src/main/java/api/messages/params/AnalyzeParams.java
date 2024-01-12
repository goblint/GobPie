package api.messages.params;

import java.util.Objects;

public class AnalyzeParams {

    boolean reset;

    public AnalyzeParams(boolean reset) {
        this.reset = reset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalyzeParams that = (AnalyzeParams) o;
        return reset == that.reset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reset);
    }
}
