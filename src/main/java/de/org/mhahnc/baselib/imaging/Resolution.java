package de.org.mhahnc.baselib.imaging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.org.mhahnc.baselib.util.IFromString;

/**
 * To store and process resolution information.
 */
public class Resolution {
    /** Width component of the resolution. Must be a positive number. */
    public int width;
    /** Height component of the resolution. Must be a positive number. */
    public int height;

    /**
     * Default ctor.
     * @param width Width component of the resolution.
     * @param height Height component of the resolution.
     */
    public Resolution(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /** The maximum width supported. */
    public static int MAX_WIDTH = 16384;
    /** The maximum height supported. */
    public static int MAX_HEIGHT = 16384;

    /** @see java.lang.Object#toString() */
    public String toString() {
        return String.format("%d,%d", this.width, this.height);
    }

    static Pattern _pattern;
    static {
        _pattern = Pattern.compile("^\\s*(\\d+)\\s*,\\s*(\\d+)\\s*$");
    }

    /** @see java.lang.Object#equals(java.lang.Object) */
    public boolean equals(Object obj) {
        if (obj instanceof Resolution r) {
            return r.width == this.width && r.height == this.height;
        }
        return false;
    }

    /** @see java.lang.Object#hashCode() */
    public int hashCode() {
        // the perfect hashcode
        return (this.width << 16) | (this.height & 0x0ffff);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Factory for deserialization from a string. The parser is capable to
     * interpret the "{width}x{height}" format, which is also used in the
     * toString() method of this class.
     */
    public static IFromString<Resolution> fromString = expr -> {
        Matcher m = _pattern.matcher(expr);
        try {
            if (m.find()) {
                return new Resolution(Integer.parseInt(m.group(1)),
                                      Integer.parseInt(m.group(2)));
            }
        }
        catch (NumberFormatException nfe) {
        }
        return null;
    };
}
