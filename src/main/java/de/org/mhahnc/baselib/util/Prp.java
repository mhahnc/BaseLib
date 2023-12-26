package de.org.mhahnc.baselib.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Data type support and helpers for all things properties.
 */
public class Prp {
    private Prp() { }

    public static abstract class Item<T> {
        protected String name;
        protected T dflt;
        public Item(String name, T dflt) { this.name = name; this.dflt = dflt; }
        public String name() { return this.name; }
        public abstract boolean validate(String raw);
        public T dflt() { return this.dflt; }
        public void set(Properties p, T val) { p.put(this.name, val.toString()); }
        public abstract T get(Properties props);
        public T get() { return get(global()); }
    }

    public static class Bool extends Item<Boolean> {
        public Bool(String name, boolean dflt) { super(name, dflt); }
        public boolean validate(String raw)  { return null != raw; }
        public Boolean get(Properties props) {
            return Boolean.parseBoolean(props.getProperty(this.name, this.dflt.toString()));
        }
    }
    public static class Int extends Item<Integer> {
        public Int(String name, int dflt) { super(name, dflt); }
        public boolean validate(String raw) {
            try { Integer.parseInt(raw); return true; }
            catch (Exception e)        { return false; } }
        public Integer get(Properties props) {
            return Integer.parseInt(props.getProperty(this.name, this.dflt.toString()));
        }
    }
    public static class Lng extends Item<Long> {
        public Lng(String name, long dflt) { super(name, dflt); }
        public boolean validate(String raw) {
            try { Long.parseLong(raw); return true; }
            catch (Exception e)      { return false; } }
        public Long get(Properties props) {
            return Long.parseLong(props.getProperty(this.name, this.dflt.toString()));
        }
    }
    public static class Str extends Item<String> {
        public Str(String name, String dflt) { super(name, dflt); }
        public boolean validate(String raw) { return null != raw; }
        public String get(Properties props) { return props.getProperty(this.name, this.dflt); };
    }

    /**
     * To read out a subrange of properties which all share a common prefix.
     * Together with a class name like naming scheme this can be used e.g. to
     * load properties like "module.this=3" and "module.that=4" together if the
     * prefix given is "module.".
     * @param props The original properties.
     * @param prefix The prefix.
     * @return The matching properties, without their original prefixes!
     */
    public static Properties selectByPrefix(Properties props, String prefix) {
        synchronized(props) {
            Properties result = new Properties();

            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    if (key.startsWith(prefix)) {
                        result.put(
                                key.substring(prefix.length()),
                                entry.getValue());
                    }
                }
            }
            return result;
        }
    }

    /**
     * Overloads properties from a parameter list. Each parameter entry must be
     * prefixed with a '-' character, with the '=' separating key from value,
     * e.g. "-color=blue". On the first non-prefix parameter the function stops
     * parsing and returns the index. Useful to parse command line properties
     * and then continue with e.g. file path parameters.
     * @param props The properties to overload.
     * @param params The parameter list. The key parts will be trimmed.
     * @return Index of the first parameter without the '-' prefix. If all of
     * the parameters are prefixed the value then has the value params.length
     * and thus is not a valid array index anymore. If a parameter with an
     * invalid syntax is encountered its index+1 as a negative number gets
     * returned.
     */
    public static int overload(Properties props, String[] params) {
        int result = 0;
        synchronized (props) {
            for (; result < params.length; result++) {
                String param = params[result];
                if (0 == param.length() || '-' != param.charAt(0)) {
                    break;
                }
                int idx = param.indexOf('=');
                if (-1 == idx) {
                    return -(result + 1);
                }
                String key = param.substring(1, idx).trim();
                if (0 == key.length()) {
                    return -(result + 1);
                }
                String value = param.substring(idx + 1);
                props.put(key, value);
            }
        }
        return result;
    }

    /**
     * Loads properties from a local file, catches errors for easier handling.
     * @param props The properties to load to.
     * @param fl The file from where to load from.
     * @return True if the operation succeeded or false if any error occurred.
     */
    public static boolean loadFromFile(Properties props, File fl) {
        boolean result = false;
        synchronized(props) {
            if (fl.exists()) {
                InputStream is = null;
                try {
                    is = new FileInputStream(fl);
                    props.load(is);
                    result = true;
                }
                catch (IOException ioe) {
                }
                catch (IllegalArgumentException iae) {
                }
                finally {
                    if (null != is) {
                        try {
                            is.close();
                        }
                        catch (IOException ioe) {
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Saves the current properties to a file (its handle will be closed even
     * if an error occurred.
     * @param props The properties to save.
     * @param fl The file to save to.
     * @param overwrite True to overwrite the file if it exists.
     * @param comment Comment to put in the top line of the file. Can be null.
     * @return False if an error occurred or overwriting was prevented.
     */
    public static boolean saveToFile(
            Properties props, File fl, boolean overwrite, String comment) {
        if (!overwrite && fl.exists()) {
            return false;
        }
        OutputStream os = null;
        try {
            os = new FileOutputStream(fl);
            synchronized(props) {
                props.store(os, null == comment ? "" : comment);
            }
            return true;
        }
        catch (IOException ioe) {
            return false;
        }
        finally {
            if (null != os) {
                try {
                    os.close();
                }
                catch (IOException ignored) {
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Gives access to global properties, to be shared across the whole process.
     * @return The global properties.
     */
    public final static Properties global() {
        return _global;
    }

    private static Properties _global = new Properties();

    ///////////////////////////////////////////////////////////////////////////

    public static abstract class Registry {
        protected abstract Iterator<Class<? extends Item<?>>> itemClasses();

        @FunctionalInterface
        public interface Itr {
            boolean onItem(Item<?> item);
        }

        public boolean iterate(Itr itr) throws Exception {
            Iterator<Class<? extends Item<?>>> i = itemClasses();
            while (i.hasNext()) {
                Class<? extends Item<?>> itemClass = i.next();
                Item<?> item = itemClass.getDeclaredConstructor().newInstance();
                if (!itr.onItem(item)) {
                    return false;
                }
            }
            return true;
        }

        public Item<?> get(final String name) throws Exception {
            final VarRef<Item<?>> result = new VarRef<>();
            iterate(item -> {
                if (item.name().equals(name)) {
                    result.v = item;
                    return false;
                }
                return true;
            });
            return result.v;
        }

        public void complete(final Properties props) throws Exception {
            iterate(item -> {
                if (!props.contains(item.name())) {
                    props.put(item.name(), item.dflt().toString());
                }
                return true;
            });
        }

        public void validate(final Properties props) throws Exception {
            final VarRef<String> invalid = new VarRef<>();
            if (!iterate(item -> {
                Object value = props.get(item.name());
                if (null != value) {
                    if (!item.validate(value.toString())) {
                        invalid.v = item.name();
                        return false;
                    }
                }
                return true;
            })) {
                throw new Exception(invalid.v);
            }
        }
    }
}
