package de.org.mhahnc.baselib.util;

public class VarLong {
    public VarLong() { }
    public VarLong(long v) {
        this.v = v;
    }
    public long v;
    public String toString() {
        return String.valueOf(this.v);
    }
}
