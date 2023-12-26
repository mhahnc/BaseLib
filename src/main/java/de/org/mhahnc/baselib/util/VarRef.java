package de.org.mhahnc.baselib.util;

public class VarRef<T> {
    public VarRef() { }
    public VarRef(T v) {  this.v = v; };
    public T v;
    public String toString() {
        return null == this.v ? "(null)" : this.v.toString();
    }
}
