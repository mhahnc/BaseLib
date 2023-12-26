package de.org.mhahnc.baselib.util;

/** Attempt to implement generic multiple objects containers. */
public abstract class Combo<T> {
    public final T t;

    public Combo(T t) {
        this.t = t;
    }

    public static class Two<T, U> extends Combo<T> {
        public final U u;
        public Two(T t, U u) {
            super(t);
            this.u = u;
        }
    }

    public static class Three<T, U, V> extends Two<T, U> {
        public final V v;
        public Three(T t, U u, V v) {
            super(t, u);
            this.v = v;
        }
    }
}
