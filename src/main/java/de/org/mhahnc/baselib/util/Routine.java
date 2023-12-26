package de.org.mhahnc.baselib.util;

public interface Routine {
    @FunctionalInterface
    public interface Arg0<T> {
        T call();
    }
    @FunctionalInterface
    public interface Arg1<T, U> {
        T call(U arg);
    }
    @FunctionalInterface
    public interface Arg2<T, U, V> {
        T call(U arg1, V arg2);
    }
    @FunctionalInterface
    public interface Args<T, U> {
        T call(U[] args);
    }
    @FunctionalInterface
    public interface Arg1E<T, U> {
        T call(U arg) throws Exception;
    }
    @FunctionalInterface
    public interface Arg2E<T, U, V> {
        T call(U arg, V arg2) throws Exception;
    }
    @FunctionalInterface
    public interface ArgsE<T, U> {
        T call(U[] args) throws Exception;
    }
}
