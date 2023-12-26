package de.org.mhahnc.baselib.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MiscUtils {

    ///////////////////////////////////////////////////////////////////////////

    public final static CharSequence fillChars(int len, char c) {
        final char[] str = new char[len];
        Arrays.fill(str, c);
        return new CharSequence() {
            public char charAt(int index) {
                return str[index];
            }
            public int length() {
                return str.length;
            }
            public CharSequence subSequence(int start, int end) {
                return new String(str, start, end - start);
            }
            public String toString() {
                return new String(str);
            }
        };
    }

    public final static String fillString(int len, char c) {
        return fillChars(len, c).toString();
    }

    ///////////////////////////////////////////////////////////////////////////

    public final static String[] csvLoad(String lst, boolean removeEmpty) {
        // TODO: support all of the CSV escaping, quotes, etc...
        final String[] l = lst.split(",");
        List<String> result = new ArrayList<>(l.length);
        for (int i = 0, c = l.length; i < c; i++) {
            String s = l[i].trim();
            if (removeEmpty && 0 == s.length()) {
                continue;
            }
            result.add(s);
        }
        return result.toArray(new String[result.size()]);
    }

    public final static String csvSave(String[] items) {
        final StringBuilder result = new StringBuilder();
        for (int i = 0, c = items.length; i < c; i++) {
            if (0 < i) result.append(",");
            result.append(items[i].trim());
        }
        return result.toString();
    }

    ///////////////////////////////////////////////////////////////////////////

    public final static File getExecutableLocation(Class<?> clazz) {
        String cp = clazz.getName();
        cp = cp.replaceAll("\\.", "/") + ".class";
        URL url = ClassLoader.getSystemResource(cp);
        if (null == url) {
            return null;
        }
        if (url.getProtocol().equalsIgnoreCase("jar")) {
            try {
                url = new URL(url.getPath());
            }
            catch (MalformedURLException mue) {
                return null;
            }
        }
        if (!url.getProtocol().equalsIgnoreCase("file")) {
            return null;
        }
        String path = url.getPath();
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        if (cp.length() + 1 > path.length()) {
            return null;
        }
        if ('/' == path.charAt(0) && File.separatorChar == '\\') {
            path = path.substring(1);
        }
        if (!path.endsWith(cp)) {
            return null;
        }
        path = path.substring(0, path.length() - cp.length());
        File result = new File(path);
        if (!result.exists()) {
            result = result.getParentFile();
            if (null == result || !result.exists()) {
                return null;
            }
        }
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String makePropertyFileName(String name) {
        return '\\' == File.separatorChar ?
            name + ".properties" :
            "." + name + "rc";
    }

    public static File determinePropFile(Class<?> mainClass, String name, boolean writeProbe) {
        name = makePropertyFileName(name);
        if (!MiscUtils.underMacOS()) {
            File exePath = getExecutableLocation(mainClass);
            if (null != exePath) {
                File result = new File(exePath, name);
                if (result.exists()) {
                    return result;
                }
                else if (writeProbe) {
                    // check if we can write to the executable location, leave
                    // an empty file behind if so (call it a reservation) ...
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(result);
                    }
                    catch (IOException ioe) {
                    }
                    finally {
                        if (null != fos) {
                            try {
                                fos.close();
                            }
                            catch (IOException ignored) {
                            }
                        }
                    }
                    if (null != fos) {
                        return result;
                    }
                }
            }
        }
        return new File(System.getProperty("user.home"), name);
    }

    ///////////////////////////////////////////////////////////////////////////

    public static void writeFile(File fl, byte[] data) throws IOException {
        FileOutputStream fos = new FileOutputStream(fl);
        try {
            fos.write(data);
        }
        finally {
            fos.close();
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    public static byte[] readInputStream(InputStream ins) throws IOException {
        final byte[] buf = new byte[4096];
        try {
            ByteArrayOutputStream baos = new
            ByteArrayOutputStream(buf.length);
            for(;;) {
                int read = ins.read(buf);
                if (-1 == read) {
                    break;
                }
                baos.write(buf, 0, read);
            }
            baos.close();
            return baos.toByteArray();
        }
        finally {
            ins.close();
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    public static byte[] readFile(File fl) throws IOException {
        return readInputStream(new FileInputStream(fl));
    }

    ///////////////////////////////////////////////////////////////////////////

    public static Calendar calendarFromMillis(final long millis) {
        Calendar result = Calendar.getInstance();
        result.setTimeInMillis(millis);
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String currentMethod() {
        try {
            throw new Exception();
        }
        catch (Exception e) {
            StackTraceElement[] stack = e.getStackTrace();
            if (null == stack || stack.length < 2) {
                return null;
            }
            return stack[1].getMethodName();
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    public static int[] uniqueRandomIndexes(final int count, Random rnd) {
        final int[] result = new int[count];

        for (int i = 0; i < count; i++) {
            result[i] = i;
        }

        for (int i = 0; i < count; i++) {
            final int j = rnd.nextInt(count);
            final int k = result[i];
            result[i] = result[j];
            result[j] = k;
        }

        return result;
    }

    ///////////////////////////////////////////////////////////////////////////

    public static Long factoredStringToULong(String expr) {
        if (0 == expr.length()) {
            return null;
        }
        int end = expr.length() - 1;
        final long factor = switch (expr.charAt(end)) {
            case 'P' -> 1000L * 1000 * 1000 * 1000 * 1000;
            case 'p' -> 1024L * 1024 * 1024 * 1024 * 1024;
            case 'T' -> 1000L * 1000 * 1000 * 1000;
            case 't' -> 1024L * 1024 * 1024 * 1024;
            case 'G' -> 1000L * 1000 * 1000;
            case 'g' -> 1024L * 1024 * 1024;
            case 'M' -> 1000L * 1000;
            case 'm' -> 1024L * 1024;
            case 'K' -> 1000L;
            case 'k' -> 1024L;
            default -> {
                end++;
                yield 1L;
            }
        };
        for (int pos = 0; pos < end; pos++) {
            if (!Character.isDigit(expr.charAt(pos))) {
                return null;
            }
        }
        try {
            return factor * Long.parseLong(expr.substring(0, end));
        }
        catch (NumberFormatException nfe) {
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String dumpError(Throwable err) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        err.printStackTrace(pw);
        pw.flush();
        return err.getLocalizedMessage() + "\n" + sw.getBuffer().toString();
    }

    ///////////////////////////////////////////////////////////////////////////

    final static Map<String, Long> STRTOSZ_PFX = new HashMap<>();
    static {
        STRTOSZ_PFX.put(""  , 1L);
        STRTOSZ_PFX.put("k" , 1000L);
        STRTOSZ_PFX.put("m" , 1000L*1000L);
        STRTOSZ_PFX.put("g" , 1000L*1000L*1000L);
        STRTOSZ_PFX.put("t" , 1000L*1000L*1000L*1000L);
        STRTOSZ_PFX.put("p" , 1000L*1000L*1000L*1000*1000L);
        STRTOSZ_PFX.put("ki", 1024L);
        STRTOSZ_PFX.put("mi", 1024L*1024L);
        STRTOSZ_PFX.put("gi", 1024L*1024L*1024L);
        STRTOSZ_PFX.put("ti", 1024L*1024L*1024L*1024L);
        STRTOSZ_PFX.put("pi", 1024L*1024L*1024L*1024L*1024L);
    }
    final static BigInteger STRTOSZ_MAXLONG = new BigInteger(Long.toString(Long.MAX_VALUE));

    public static Long strToUSz(String expr) {
        expr = expr.trim();
        int len = expr.length();
        if (0 == len) {
            return -1L;
        }
        int pos = 0;
        while (pos < len) {
            char c = expr.charAt(pos);
            if ('0' > c || c > '9') {
                break;
            }
            pos++;
        }
        final String num = expr.substring(0, pos);
        final String pfx = expr.substring(pos).trim().toLowerCase();

        long result;
        try {
            result = Long.parseLong(num);
        }
        catch (NumberFormatException nfe) {
            return -2L;
        }
        final Long mul = STRTOSZ_PFX.get(pfx);
        if (null == mul) {
            return -3L;
        }
        BigInteger biMul    = new BigInteger(Long.toString(mul));
        BigInteger biResult = new BigInteger(Long.toString(result));
        biResult = biResult.multiply(biMul);
        if (1 == biResult.compareTo(STRTOSZ_MAXLONG)) {
            return -4L;
        }
       return result * mul;
    }

    public static String uszToStr(long usz, boolean thousand, boolean gap, int decs) {
        long div = thousand ? 1000 : 1024;
        Long rem = null;
        Character s2 = null;
        for (char s : "kmgtp".toCharArray()) {
            if (0 == usz / div) {
                break;
            }
            rem = usz % div;
            usz /= div;
            s2 = s;
        }
        if (null == rem) {
            return Long.toString(usz);
        }
        String sfx = (gap ? " " : "") + s2 + (thousand ? "" : "i");
        if (1 > decs) {
            return Long.toString(usz) + sfx;
        }
        String fmt = String.format("%%.%df", Math.min(2, decs));
        double result = usz + ((double)rem / (double)div);
        return String.format(fmt, result) + sfx;
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String printTime(long millis) {
        if (0 == millis) {
            return "0s";
        }
        long h  = millis / 3600000L; millis %= 3600000L;
        long m  = millis /   60000L; millis %=   60000L;
        long s  = millis /    1000L; millis %=    1000L;
        double sd = (s  * 1000 + millis) / 1000.0;
        if (0 == h) {
            if (0 == m) {
                if (0 == s && millis < 500) {
                    return String.format("%dms", millis);
                }
                return String.format("%.1fs", sd);
            }
            return String.format("%dm %.1fs", m, sd);
        }
        return String.format("%dh %dm %.1fs", h, m, sd);
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String copyrightYear(int start, Calendar now) {
        int year = now.get(Calendar.YEAR);
        if (year <= start) {
            return String.valueOf(start);
        }
        return String.format("%d-%d", start, year);
    }

    ///////////////////////////////////////////////////////////////////////////

    @FunctionalInterface
    public interface LineModifier {
        String modify(String rawLn);
    }

    public static String modifyLines(String raw, int maxLines, LineModifier lmod) {
        LineNumberReader lnr = new LineNumberReader(new StringReader(raw));
        StringBuilder result = new StringBuilder(raw.length());
        for(int i = 0, c = 0 > maxLines ? Integer.MAX_VALUE : maxLines; i < c; i++) {
            String ln = null;
            try {
                ln = lnr.readLine();
            }
            catch (IOException ignored) {
            }
            if (null == ln) {
                break;
            }
            if (0 < i) {
                result.append('\n');
            }
            result.append(lmod.modify(ln));
        }
        return result.toString();
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String stackTraceToStr(Throwable err) {
        StringWriter result = new StringWriter();
        PrintWriter pwr = new PrintWriter(result);
        err.printStackTrace(pwr);
        pwr.flush();
        pwr.close();
        return result.toString();
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean mixedCase(String s) {
        boolean lc = false;
        boolean uc = false;
        for (int pos = 0, len = s.length(); pos < len; pos++) {
            char c = s.charAt(pos);
                 if (Character.isLowerCase(c)) lc = true;
            else if (Character.isUpperCase(c)) uc = true;
            if (lc && uc) {
                return true;
            }
        }
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String limitString(String s, int lim, String etc) {
        int l = s.length();
        if (l <= lim) {
            return s;
        }
        return s.substring(0, lim) + etc;
    }

    ///////////////////////////////////////////////////////////////////////////

    public static Long __TEST_uncaught_now;

    public static void dumpUncaughtError(Throwable err, String name) {
        err.printStackTrace(System.err);
        try {
            long now = null == __TEST_uncaught_now ? System.currentTimeMillis() :
                               __TEST_uncaught_now;

            String report = String.format("%s\n%s\n%s\n",
                    new Date(now),
                    err.getLocalizedMessage(),
                    MiscUtils.stackTraceToStr(err));

            MiscUtils.writeFile(
                    new File(System.getProperty("java.io.tmpdir"),
                             String.format("%s_uncaught_%d.txt", name, now)),
                    report.getBytes());
        }
        catch (Throwable ignored) {
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    @Deprecated
    public static boolean underOSX() {
        return underMacOS();
    }

    public static boolean underMacOS() {
        String osn = System.getProperty("os.name");
        return null != osn && osn.toLowerCase().startsWith("mac");
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean underWindows() {
        String osn = System.getProperty("os.name");
        return osn.toLowerCase().contains("windows");
    }
}
