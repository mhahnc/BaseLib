package de.org.mhahnc.baselib.util;

/**
 * Generic interface for objects which can be deserialized from a string.
 * @param <T> Any class type.
 */
@FunctionalInterface
public interface IFromString<T> {
    /**
     * Create an instance of an object out of a string expression.
     * @param expr The expression to parse.
     * @return New Object instance or null if the expression was malformed.
     */
    public T create(String expr);
}
