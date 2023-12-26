package de.org.mhahnc.baselib.test.util;

public class TestError extends Error {
    private static final long serialVersionUID = -8480743339110414638L;

    public TestError(String fmt, Object... args) {
        super(String.format(fmt, args));
    }
}
