package de.org.mhahnc.baselib.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Logging support. Not too fancy. Levels compatible with Log4j. */
public class Log {
    public enum Level {
        TRACE("TRC"),
        DEBUG("DBG"),
        INFO ("INF"),
        WARN ("WRN"),
        ERROR("ERR"),
        FATAL("FTL");

        Level(String token) {
            this.token = token;
        }
        public String token() {
            return this.token;
        }
        final String token;
    }

    ///////////////////////////////////////////////////////////////////////////

    static List<PrintStream>      _printers = new ArrayList<>();
    static AtomicReference<Level> _level    = new AtomicReference<>();

    ///////////////////////////////////////////////////////////////////////////

    public static void printf(Level lvl, String ctx, String fmt, Object... args) {
        if (_level.get() == null || !pass(lvl)) {
            return;
        }

        final Calendar c = Calendar.getInstance();

        final String msg = String.format(
            "%s %04d.%02d.%02d-%02d:%02d:%02d.%03d <%d> [%s] %s",
            lvl.token(),
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH),
            c.get(Calendar.HOUR),
            c.get(Calendar.MINUTE),
            c.get(Calendar.SECOND),
            c.get(Calendar.MILLISECOND),
            Thread.currentThread().getId(),
            ctx,
            String.format(fmt, args));

        synchronized(_printers) {
            for (PrintStream printer : _printers) {
                printer.println(msg);
            }
        }
    }

    public static void print(Level lvl, String ctx, String msg) {
        printf(lvl, ctx, "%s", msg);
    }

    public static void exception(Level lvl, String ctx, Throwable err) {
        final StringBuilder msg = new StringBuilder();
        msg.append(err.toString());
        msg.append('\n');
        for (StackTraceElement ste : err.getStackTrace()) {
            msg.append('\t');
            msg.append(ste.toString());
            msg.append('\n');
        }
        print(Level.ERROR, ctx, msg.toString());
    }

    public static void level(Level lvl) {
        _level.set(lvl);
    }

    public static boolean pass(Level lvl) {
        return lvl.ordinal() >= _level.get().ordinal();
    }

    public static void addPrinter(PrintStream printer) {
        synchronized(_printers) {
            _printers.add(printer);
        }
    }

    public static void flush() {
        synchronized(_printers) {
            for (PrintStream printer : _printers) {
                printer.flush();
            }
        }
    }

    public static void reset() {
        synchronized(_printers) {
            for (PrintStream printer : _printers) {
                printer.flush();
                printer.close();
            }
            _printers.clear();
        }
        level(null);
    }

    ///////////////////////////////////////////////////////////////////////////

    protected String ctx;

    public Log(String ctx) {
        this.ctx = ctx;
    }

    public void trace (String msg                ) { print (Level.TRACE, this.ctx, msg      ); }
    public void tracef(String fmt, Object... args) { printf(Level.TRACE, this.ctx, fmt, args); }
    public void debug (String msg                ) { print (Level.DEBUG, this.ctx, msg      ); }
    public void debugf(String fmt, Object... args) { printf(Level.DEBUG, this.ctx, fmt, args); }
    public void info  (String msg                ) { print (Level.INFO , this.ctx, msg      ); }
    public void infof (String fmt, Object... args) { printf(Level.INFO , this.ctx, fmt, args); }
    public void warn  (String msg                ) { print (Level.WARN , this.ctx, msg      ); }
    public void warnf (String fmt, Object... args) { printf(Level.WARN , this.ctx, fmt, args); }
    public void error (String msg                ) { print (Level.ERROR, this.ctx, msg      ); }
    public void errorf(String fmt, Object... args) { printf(Level.ERROR, this.ctx, fmt, args); }
    public void fatal (String msg                ) { print (Level.FATAL, this.ctx, msg      ); }
    public void fatalf(String fmt, Object... args) { printf(Level.FATAL, this.ctx, fmt, args); }
}
