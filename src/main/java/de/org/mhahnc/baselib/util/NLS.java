package de.org.mhahnc.baselib.util;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public abstract class NLS {
    public final static String DEFAULT_LANG_ID = "en";

    public static class Str {
        Str(String s) {
            this.s = s;
        }
        public final String s() {
            return this.s;
        }
        protected String s;
        public String fmt(Object... args) {
            return String.format(this.s, args);
        }
        @Override
        public String toString() {
            return this.s;
        }
        @Override
        public int hashCode() {
            return this.s.hashCode();
        }
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Str str) {
                obj = str.s;
            }
            return this.s.equals(obj);
        }
    };

    ///////////////////////////////////////////////////////////////////////////

    public static class Reg {
        Set<Class<? extends NLS>> nlss = new HashSet<>();

        public static Reg instance() {
            return _instance;
        }
        final static Reg _instance = new Reg();

        public boolean register(Class<? extends NLS> nls) {
            boolean result = this.nlss.add(nls);
            if (result && null != this.id.get()) {
                try {
                    loadNLS(nls, id());
                }
                catch (BaseLibException ble) {
                    ble.printStackTrace(System.err);
                    result = false;
                }
            }
            return result;
        }

        @FunctionalInterface
        public interface Listener {
            void onLoaded();
        }
        Set<Listener> ls = new HashSet<>();

        public boolean addListener(Listener l) {
            return this.ls.add(l);
        }

        public boolean removeListener(Listener l) {
            return this.ls.remove(l);
        }

        public void load(String id) throws BaseLibException {
            id = null == id ? DEFAULT_LANG_ID : id;
            this.id.set(id);
            for (Class<? extends NLS> nls : this.nlss) {
                loadNLS(nls, id);
            }
            for (Listener l : this.ls.toArray(new Listener[0])) {
                l.onLoaded();
            }
        }
        AtomicReference<String> id = new AtomicReference<>();

        public String id() {
            return this.id.get();
        }

        public void reset() {
            this.nlss.clear();
            this.ls  .clear();
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    static String strName(Field f) {
        if (f.getType().equals(Str.class)) {
            int m = f.getModifiers();
            String result = f.getName();
            if ((m & Modifier.PUBLIC) == Modifier.PUBLIC &&
                (m & Modifier.STATIC) == Modifier.STATIC &&
                result.toUpperCase().equals(result)) {
                return result;
            }
        }
        return null;
    }

    static void loadNLS(Class<? extends NLS> clz, String id) throws BaseLibException {
        loadNLS(clz, id, "UTF-8");
    }

    static void loadNLS(Class<? extends NLS> clz, String id, String encoding) throws BaseLibException {
        try {
            String res = "NLS_" + id + ".properties";
            Properties p = new Properties();
            p.load(new InputStreamReader(clz.getResourceAsStream(res), Charset.forName(encoding)));
            Map<String, Field> nfmap = new HashMap<>();
            for (Field f : clz.getDeclaredFields()) {
                String n = strName(f);
                if (null != n) {
                    nfmap.put(n, f);
                }
            }
            for (Map.Entry<Object, Object> e : p.entrySet()) {
                String nm = e.getKey().toString();
                Field f = nfmap.remove(nm);
                if (null == f) {
                    throw new BaseLibException("unknown NLS string %s", nm);
                }
                Object obj = f.get(null);
                String s = e.getValue().toString();
                if (null == obj) {
                    f.set(null, new Str(s));
                }
                else {
                    ((Str)obj).s = s;
                }
            }
            if (0 < nfmap.size()) {
                StringBuilder sb = new StringBuilder();
                for (String n : nfmap.keySet()) {
                    if (0 < sb.length()) sb.append(", ");
                    sb.append(n);
                }
                throw new BaseLibException("missing NLS strings: %s", sb);
            }
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
            throw new BaseLibException("NLS load error for %s (%s)",
                                       clz.getName(), e.getMessage());
        }
    }
}
