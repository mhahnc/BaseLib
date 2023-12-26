package de.org.mhahnc.baselib.util;

public class VarInt {
    public VarInt() { }
    public VarInt(int v) {
        this.v = v;
    }
    public int v;
    public String toString() {
        return String.valueOf(this.v);
    }
}
