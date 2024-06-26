package api.messages.params;

import java.util.Objects;

public class Params {

    private String fname;

    public Params() {
    }

    public Params(String fname) {
        this.fname = fname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Params params = (Params) o;
        return Objects.equals(fname, params.fname);
    }
}
