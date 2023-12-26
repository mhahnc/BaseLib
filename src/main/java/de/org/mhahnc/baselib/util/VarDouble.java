package de.org.mhahnc.baselib.util;

public class VarDouble {
    public VarDouble() { }
    public VarDouble(double v) {
        this.v = v;
    }
    public double v;
    public String toString() {
        return String.valueOf(this.v);
    }
}
