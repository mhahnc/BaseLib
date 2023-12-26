package de.org.mhahnc.baselib.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CmdLnParser {
    public static class Error extends Exception {
        private static final long serialVersionUID = 2734889991222358663L;
        public Error(String msg) { super(msg); }
    }

    public final static String OPT_ESC    = "---";
    public final static char   OPT_PFX    = '-';
    public final static String OPT_PFX_L  = "--";
    public final static char   OPT_ASSIGN = '=';

    Map<String, Prp.Item<?>> props   = new HashMap<>();
    List<String>               params  = new ArrayList<>();
    Properties                 options = new Properties();

    public void addProp(String name, Prp.Item<?> prop) {
        this.props.put(name, prop);
    }

    static String unescape(String s) {
        return s.startsWith("---") ? s.substring(2) : null;
    }

    public String[] parse(String[] args, boolean skipParams, boolean skipEmpty) throws Error {
        this.params .clear();
        this.options.clear();

        List<String> result = new ArrayList<>();
        for (final String arg : args) {
            if (0 == arg.length() && skipEmpty) {
                continue;
            }
            String uarg = unescape(arg);
            if (null == uarg &&
                arg.length() > 0 &&
                arg.charAt(0) == OPT_PFX) {
                int pos = arg.indexOf(OPT_ASSIGN);
                final String n, v;
                if (-1 != pos) {
                    n = arg.substring(0, pos);
                    v = arg.substring(pos + 1);
                }
                else {
                    n = arg;
                    v = null;
                }
                Prp.Item<?> prop = this.props.get(n);
                if (null == prop) {
                    result.add(arg);
                }
                else {
                    if (null == v) {
                        if (prop instanceof Prp.Bool) {
                            this.options.put(prop.name(), Boolean.TRUE.toString());
                            continue;
                        }
                        throw new Error(arg);
                    }
                    if (!prop.validate(v)) {
                        throw new Error(arg);
                    }
                    this.options.put(prop.name(), v);
                }
            }
            else {
                (skipParams ? result : this.params).add(null == uarg ? arg : uarg);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public Properties options() {
        return this.options;
    }

    public List<String> params() {
        return this.params;
    }
}
