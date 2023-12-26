package de.org.mhahnc.baselib.io;

import java.util.regex.Pattern;

/**
 * File system filter supporting the regular * and ? mask expressions.
 */
public class DefaultFileSystemFilter implements FileSystem.Filter {
    public final static char WILDCARD_ALL = '*';
    public final static char WILDCARD_ONE = '?';

    Pattern p;

    public DefaultFileSystemFilter(String mask) {
        StringBuilder rex = new StringBuilder(mask.length() << 1);
        rex.append('^');

        final int len = mask.length();
        for (int i = 0; i < len; i++) {
            final Character c = mask.charAt(i);
            switch(c) {
                case WILDCARD_ALL: {
                    rex.append(".*?");
                    break;
                }
                case WILDCARD_ONE: {
                    rex.append(".");
                    break;
                }
                default: {
                    rex.append(Pattern.quote(c.toString()));
                    break;
                }
            }
        }
        rex.append('$');

        this.p = Pattern.compile(rex.toString());
    }

    public boolean matches(FileNode file) {
        return this.p.matcher(file.name()).matches();
    }

    public static boolean isMask(final String mask) {
        final int len = mask.length();
        for (int i = 0; i < len; i++) {
            switch(mask.charAt(i)) {
                case WILDCARD_ALL:
                case WILDCARD_ONE: {
                    return true;
                }
            }
        }
        return false;
    }
}
