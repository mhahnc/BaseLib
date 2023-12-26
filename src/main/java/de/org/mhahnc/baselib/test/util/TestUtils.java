package de.org.mhahnc.baselib.test.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.org.mhahnc.baselib.io.IOUtils;
import de.org.mhahnc.baselib.util.BinUtils;
import de.org.mhahnc.baselib.util.BytePtr;
import de.org.mhahnc.baselib.util.MiscUtils;

public class TestUtils {
    public static BytePtr fillPattern123(int len) {
        byte[] buf = new byte[1 + len + 1];

        buf[0]              =
        buf[buf.length - 1] = (byte)0xcc;

        fillPattern123(buf, 1, len);

        return new BytePtr(buf, 1, len);
    }

    public static void fillPattern123(byte[] buf) {
        fillPattern123(buf, 0, buf.length);
    }

    public static void fillPattern123(byte[] buf, int ofs, int len) {
        int counter, end;

        counter = 0;
        end = ofs + len;
        while (ofs < end) {
            buf[ofs++] = (byte)counter++;
        }
    }

    public static boolean checkPattern123(BytePtr bp) {
        return checkPattern123(bp.buf, bp.ofs, bp.len);
    }

    public static boolean checkPattern123(byte[] buf, int ofs, int len) {
        int counter, end;

        counter = 0;
        end = ofs + len;
        while (ofs < end) {
            if (buf[ofs] != (byte)counter) {
                return false;
            }
            counter++;
            ofs++;
        }
        return true;
    }

    public static void fillFile123(File fl, long len) throws IOException {
        if (fl.getName().equals(",;")) {
            fl = fl.getAbsoluteFile();
        }

        OutputStream os;
        try {
            os = new BufferedOutputStream(new FileOutputStream(fl));
        }
        catch (IOException ioe) {
            ioe.printStackTrace(System.err);
            throw ioe;
        }
        try {
            for (long l = 0; l < len; l++) {
                os.write((int)l);
            }
            os.flush();
        }
        finally {
            os.close();
        }
    }

    public static long checkFile123(File fl) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(fl));
        try {
            long result = 0;
            for (;;) {
                int b = is.read();
                if (-1 == b) {
                    return result;
                }
                if (((int)result & 0xff) != (b & 0xff)) {
                    return -1L;
                }
                result++;
            }
        }
        finally {
            is.close();
        }
    }

    public static boolean checkFill(BytePtr bp, byte value) {
        for (int i = 0, len = bp.len; i < len; i++) {
            if (bp.at(i) != value) {
                return false;
            }
        }
        return true;
    }

    public static boolean removeDir(File dir, boolean safe) {
        if (!dir.exists()) {
            return true;
        }
        if (!dir.isDirectory()) {
            return false;
        }
        if (safe && (null == dir.getParentFile() ||
                     null == dir.getParentFile().getParentFile())) {
            return false;
        }
        for (File fl : dir.listFiles()) {
            if (fl.isDirectory()) {
                removeDir(fl, false);
            }
            else {
                if (!fl.delete()) {
                    System.err.println("CANNOT REMOVE " + fl);
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static File dumpToFile(byte[] data, String fname,
                                  boolean toTmpDir) throws IOException {
        File fl = new File(fname);
        if (toTmpDir) {
            fl = new File(System.getProperty("java.io.tmpdir", null), fl.getName());
        }
        if (toTmpDir && fl.exists()) {
            if (!fl.delete()) {
                throw new IOException();
            }
        }
        OutputStream os = new FileOutputStream(fl);
        os.write(data);
        os.flush();
        os.close();
        return fl;
    }

    public static <T> List<T> itrToLst(Iterator<T> itr, boolean noDups) {
        List<T> result = new ArrayList<>();

        while (itr.hasNext()) {
            T item = itr.next();
            if (noDups && result.contains(item)) {
                return null;
            }
            result.add(item);
        }

        return result;
    }

    public static boolean inputStreamsEqual(
            InputStream is0, InputStream is1, boolean close) throws IOException {
        try {
            for(;;) {
                final int b0 = is0.read();
                final int b1 = is1.read();
                if (b0 != b1) {
                    return false;
                }
                if (-1 == b0) {
                    return true;
                }
            }
        }
        finally {
            if (close) {
                try { is0.close(); } catch (IOException ignored) { }
                try { is1.close(); } catch (IOException ignored) { }
            }
        }
    }

    public static boolean resStrValid(String res) {
        return null != res && !(res.startsWith("!") &&
                                res.endsWith  ("!"));
    }

    public static OutputStream newNulOutputStream() {
        return new OutputStream() {
            boolean closed;
            public void write(int b) throws IOException {
                if (this.closed) {
                    throw new IOException();
                }
            }
            public void close() {
                this.closed = true;
            }
        };
    }

    public static String createTempFileName(String pfx) {
        return String.format("%s_%s_%08x",
                pfx,
                System.currentTimeMillis(),
                new java.security.SecureRandom().nextInt());
    }

    public static File createTempFile(String pfx, int len) throws IOException {
        File result = new File(System.getProperty("java.io.tmpdir"),
                               createTempFileName(pfx)).getCanonicalFile();
        if (result.exists() && !result.delete()) {
            throw new IOException(result.getAbsolutePath() + " exists, cannot delete");
        }
        if (-1 != len) {
            TestUtils.fillFile123(result, len);
        }
        return result;
    }

    public static File createTempDir(String pfx) throws IOException {
        File result = createTempFile(pfx, -1);

        if (!result.mkdirs()) {
            throw new IOException(result.getAbsolutePath() + " cannot be created");
        }

        return result;
    }

    public static int pathDepth(File fl, boolean includeRoot) {
        int result = 0;
        for (;;) {
            File p = fl.getParentFile();
            if (null == p) {
                return result + (includeRoot ? 1 : 0);
            }
            result++;
            fl = p;
        }
    }

    public static String npath(String path) {
        return path.replace('/', File.separatorChar);
    }

    public static long adjustFileTimestamp(long tm) {
        if (MiscUtils.underWindows()) {
            return tm;
        }
        return tm - tm % 1000; // assuming 1 second resolution under *ix systems
    }

    public static String extractRelativePath(File parentPath, File filePath) {
        String pp = parentPath.getAbsolutePath();
        String fp = filePath  .getAbsolutePath();
        if (!fp.startsWith(pp)) {
            throw new TestError("file path '%s' does not start with parent path '%s'", fp, pp);
        }
        String rp = fp.substring(pp.length());
        return 0 == rp.length() || rp.charAt(0) != File.separatorChar ? rp : rp.substring(1);
    }

    public static boolean areFilesEqual(File fl0, File fl1) throws IOException {
        InputStream is0 = null;
        InputStream is1 = null;
        try {
            is0 = new BufferedInputStream(new FileInputStream(fl0));
            is1 = new BufferedInputStream(new FileInputStream(fl1));
            byte[] buf0 = new byte[4096];
            byte[] buf1 = buf0.clone();
            for (;;) {
                int read  = IOUtils.readAll(is0, buf0, 0, buf0.length);
                if (read != IOUtils.readAll(is1, buf1, 0, buf1.length)) {
                    return false;
                }
                for (int i = 0; i < read; i++) {
                    if (buf0[i] != buf1[i]) {
                        return false;
                    }
                }
                if (buf0.length > read) {
                    return true;
                }
            }
        }
        finally {
            if (null != is1) try { is1.close(); } catch (IOException ignored) { }
            if (null != is0) try { is0.close(); } catch (IOException ignored) { }
        }
    }

    public static String md5OfFile(File fl) throws Exception {
        MessageDigest md = MessageDigest.getInstance("md5");
        InputStream ins = null;
        try {
            ins = new BufferedInputStream(new FileInputStream(fl));
            byte[] buf = new byte[1 << 16];
            for(boolean run = true; run;) {
                int read = ins.read(buf);
                if (run = 0 < read) {
                    md.update(buf, 0, read);
                }
            }
            return BinUtils.bytesToHexString(md.digest());
        }
        finally {
            if (null != ins) ins.close();
        }
    }
}
