package de.org.mhahnc.baselib.util;

public class VarBool {
    public VarBool() { }
    public VarBool(boolean v) {
        this.v = v;
    }
    public boolean v;
    public String toString() {
        return String.valueOf(this.v);
    }
}
