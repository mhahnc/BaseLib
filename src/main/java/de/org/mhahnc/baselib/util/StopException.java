package de.org.mhahnc.baselib.util;

/**
 * Exception signaling an interruption, usually coming from the user.
 */
public class StopException extends RuntimeException {
    private static final long serialVersionUID = -6360430182390916850L;
    /** @see java.lang.Exception#Exception() */
    public StopException() { }
    /** @see java.lang.Exception#Exception(java.lang.String) */
    public StopException(String message) { super(message); }
}
