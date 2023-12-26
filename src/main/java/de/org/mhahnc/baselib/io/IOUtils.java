package de.org.mhahnc.baselib.io;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class IOUtils {
    public static byte[] readStreamBytes(InputStream ins) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        byte[] buf = new byte[4096];
        int read;
        while (0 < (read = ins.read(buf))) {
            result.write(buf, 0, read);
        }
        ins.close();
        result.close();

        return result.toByteArray();
    }

    public static int readAll(InputStream ins,
            byte[] buf, int ofs, int len) throws IOException {
        int ofs0 = ofs;
        for (int end = ofs + len; ofs < end;) {
            int read = ins.read(buf, ofs, end - ofs);
            if (-1 == read) {
                break;
            }
            ofs += read;
        }
        return ofs - ofs0;
    }

    public static int readAll(RandomAccessFile raf,
            byte[] buf, int ofs, int len) throws IOException {
        int ofs0 = ofs;
        for (int end = ofs + len; ofs < end;) {
            int read = raf.read(buf, ofs, end - ofs);
            if (-1 == read) {
                break;
            }
            ofs += read;
        }
        return ofs - ofs0;
    }

    public static File getRoot(File fl) {
        for(;;) {
            File p = fl.getParentFile();
            if (null == p) {
                return fl;
            }
            fl = p;
        }
    }

    public static boolean isRoot(File fl) {
        if (null != fl.getParent()) {
            return false;
        }
        File[] roots = File.listRoots();
        if (null == roots) {
            return false;
        }
        for (File root : roots) {
            if (root.equals(fl)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> fileToElements(File fl) {
        List<String> result = new ArrayList<>();
        for (;;) {
            String nm = fl.getName();
            File p = fl.getParentFile();
            boolean root = null == p;
            if (0 == nm.length() && root) {
                nm = fl.getPath();
                if (nm.endsWith(File.separator)) {
                    nm = nm.substring(0, nm.length() - 1);
                }
            }
            result.add(0, nm);
            if (root) {
                return result;
            }
            fl = p;
        }
    }

    public static String elementsToFileStr(List<String> elems, int start, int end, boolean absolute) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i > start) {
                sb.append(File.separatorChar);
            }
            sb.append(elems.get(i));
        }
        if (absolute && 0 == start && 1 == end) {
            sb.append(File.separatorChar);
        }
        return sb.toString();
    }

    public static StringBuilder stripRoot(File fl) {
          List<String> elems = fileToElements(fl);
          StringBuilder result = new StringBuilder();
          for (int i = 1; i < elems.size(); i++) {
              if (i > 1) {
                  result.append(File.separatorChar);
              }
              result.append(elems.get(i));
          }
          return result;
    }

    public static String[] resolveRelativePath(File fl, File cnclCurrPath) throws IOException {
        final String[] result = new String[2];

        if (".".equals(fl.getPath())) {  // (needed for testing)
            result[0] = cnclCurrPath.getCanonicalPath();
            return result;
        }

        List<String> el = new ArrayList<>();
        List<String> up = new ArrayList<>();
        for (String e : IOUtils.fileToElements(fl)) {
            if (e.equals("..")) {
                if (0 < el.size()) {
                    el.remove(el.size() - 1);
                }
                else {
                    up.add(e);
                }
            }
            else if (!e.equals(".") && 0 < e.length()) {
                el.add(e);
            }
        }

        if (0 == el.size()) {
            el = null;
        }

        if (0 < up.size()) {
            cnclCurrPath = new File(cnclCurrPath, IOUtils.elementsToFileStr(up, 0, up.size(), false));
        }

        result[0] = cnclCurrPath.getCanonicalPath();
        result[1] = null == el ? null :
                    IOUtils.elementsToFileStr(el, 0, el.size(), false);
        return result;
    }

    public static String __test_CURRENTPATH;

    public static File currentPath() throws IOException {
        return new File(null != __test_CURRENTPATH ?
                                __test_CURRENTPATH : ".").getCanonicalFile();
    }

    public static String[] extractMask(String expr) {
        String[] result = new String[2];
        int pos = expr.lastIndexOf(File.separatorChar);
        if (-1 == pos) {
            result[0] = null;
        }
        else {
            result[0] = expr.substring(0, ++pos);
            expr = expr.substring(pos);
        }
        if (DefaultFileSystemFilter.isMask(expr)) {
            result[1] = expr;
            return result;
        }
        return null;
    }

    public static boolean copy(InputStream is, OutputStream os, boolean close) throws IOException {
        boolean result = true;
        try {
            final byte[] buf = new byte[4096];
            for (;;) {
                final int read = is.read(buf);
                if (-1 == read) {
                    break;
                }
                os.write(buf, 0, read);
            }
        }
        finally {
            if (close) {
                try { os.close(); } catch (IOException ioe) { result = false; }
                try { is.close(); } catch (IOException ioe) { result = false; }
            }
        }
        return result;
    }

    public static boolean dumpBlockDevice(BlockDevice bdev, OutputStream os) {
        try {
            byte[] blk = new byte[bdev.blockSize()];
            for (long num = 0L, end = bdev.size(); num < end; num++) {
                bdev.read(num,  blk, 0);
                os.write(blk);
            }
            return true;
        }
        catch (IOException ioe) {
            return false;
        }
        finally {
            try {
                os.close();
            }
            catch (IOException ioe) {
                return false;
            }
        }
    }

    public static boolean dumpBlockDevice(BlockDevice bdev, File fl) {
        try {
            FileOutputStream fos = new FileOutputStream(fl);
            return dumpBlockDevice(bdev, new BufferedOutputStream(fos));
        }
        catch (IOException ioe) {
            return false;
        }
    }
}
