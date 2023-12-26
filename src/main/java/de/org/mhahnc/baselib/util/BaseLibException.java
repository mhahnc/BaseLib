package de.org.mhahnc.baselib.util;

public class BaseLibException extends Exception {
    private static final long serialVersionUID = -3877591951404006756L;

    public BaseLibException(Throwable cause, String fmt, Object... args) {
        super(String.format(fmt, args), cause);
    }
    public BaseLibException(String fmt, Object... args) {
        this(null, fmt, args);
    }
}
