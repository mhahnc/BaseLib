package de.org.mhahnc.baselib.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

public class BinUtils  {

    private BinUtils() {
    }

    public static boolean arraysEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        return arraysEquals(a, 0, b, 0, a.length);
    }

    public static boolean arraysEquals(
            byte[] a, int ofsA, byte[] b, int ofsB, int len) {
        int end = ofsA + len;
        while (ofsA < end) {
            if (b[ofsB++] != a[ofsA++]) {
                return false;
            }
        }
        return true;
    }

    public static int[][] copyArray(int[][] arr) {
        int[][] result = new int[arr.length][];
        for (int i = 0, c = result.length; i < c; i++) {
            result[i] = arr[i].clone();
        }
        return result;
    }

    public static String bytesToHexString(byte[] buf) {
        return bytesToHexString(buf, 0, buf.length);
    }
    public static String bytesToHexString(BytePtr bp) {
        return bytesToHexString(bp.buf, bp.ofs, bp.len);
    }
    public static String bytesToHexString(byte[] buf, int ofs, int len) {
        final char[] hextab = "0123456789abcdef".toCharArray();
        char[] result = new char[len << 1];
        for (int end = ofs + len, i = 0; ofs < end; ofs++, i += 2) {
            int b = buf[ofs];
            result[i    ] = hextab[(b >>> 4) & 0x0f];
            result[i + 1] = hextab[ b        & 0x0f];
        }
        return new String(result);
    }

    public static byte[] hexStrToBytes(String hex) {
        int len = hex.length();
        if (1 == (len & 1)) {
            return null;
        }
        byte[] result = new byte[len >> 1];
        int r = 0;
        int pos = 0;
        while (pos < len) {
            int reg = 0;
            for (int nI = 0; nI < 2; nI++) {
                reg <<= 4;
                char c = Character.toLowerCase(hex.charAt(pos++));
                if ('0' <= c && '9' >= c) {
                    reg |= c - '0';
                }
                else if ('a' <= c && 'f' >= c) {
                    reg |= (c - 'a') + 10;
                }
                else {
                    return null;
                }
            }
            result[r++] = (byte)reg;
        }
        return result;
    }

    public final static byte[] longsToBytesLE(long[] l) {
        int len = l.length;
        byte[] result = new byte[len << 3];
        for (int i = 0, pos = 0; i < len; i++) {
            long val = l[i];
            for (int j = 0; j < 8; j++) {
                result[pos++] = (byte)val;
                val >>>= 8;
            }
        }
        return result;
    }

    public final static short readInt16LE(byte[] data, int ofs) {
        return (short)(((data[ofs + 1] & 0xff) <<  8) |
                        (data[ofs    ] & 0xff));
    }

    public final static void writeInt16LE(short value, byte[] data, int ofs) {
        data[ofs    ] = (byte)(value      );
        data[ofs + 1] = (byte)(value >>> 8);
    }

    public final static short readInt16BE(byte[] data, int ofs) {
        return (short)(((data[ofs    ] & 0xff) << 8) |
                        (data[ofs + 1] & 0xff));
    }

    public final static void writeInt16BE(short value, byte[] data, int ofs) {
        data[ofs + 1] = (byte)(value      );
        data[ofs    ] = (byte)(value >>> 8);
    }

    public final static int readInt32LE(byte[] data, int ofs) {
        return ( data[ofs + 3]         << 24) |
               ((data[ofs + 2] & 0xff) << 16) |
               ((data[ofs + 1] & 0xff) <<  8) |
                (data[ofs    ] & 0xff);
    }

    public final static void writeInt32LE(int value, byte[] data, int ofs) {
        data[ofs    ] = (byte)(value       );
        data[ofs + 1] = (byte)(value >>>  8);
        data[ofs + 2] = (byte)(value >>> 16);
        data[ofs + 3] = (byte)(value >>> 24);
    }

    public final static int readInt32BE(byte[] data, int ofs) {
        return ( data[ofs    ]         << 24) |
               ((data[ofs + 1] & 0xff) << 16) |
               ((data[ofs + 2] & 0xff) <<  8) |
                (data[ofs + 3] & 0xff);
    }

    public final static void writeInt32BE(int value, byte[] data, int ofs) {
        data[ofs + 3] = (byte)(value       );
        data[ofs + 2] = (byte)(value >>>  8);
        data[ofs + 1] = (byte)(value >>> 16);
        data[ofs    ] = (byte)(value >>> 24);
    }

    public final static long readInt64LE(byte[] data, int ofs) {
        return (      readInt32LE(data, ofs    ) & 0x0ffffffffL) |
               ((long)readInt32LE(data, ofs + 4) << 32);
    }

    public final static void writeInt64LE(long value, byte[] data, int ofs) {
        writeInt32LE((int)(value >>> 32), data, ofs + 4);
        writeInt32LE((int)(value       ), data, ofs);
    }

    public final static long readInt64BE(byte[] data, int ofs) {
        return (      readInt32BE(data, ofs + 4) & 0x0ffffffffL) |
               ((long)readInt32BE(data, ofs    ) << 32);
    }

    public final static void writeInt64BE(long value, byte[] data, int ofs) {
        writeInt32BE((int)(value >>> 32), data, ofs);
        writeInt32BE((int)(value       ), data, ofs + 4);
    }

    public final static void xorInt64OverBytesLE(long val, byte[] data, int ofs) {
        // optimized for 32bit

        int tmp = (int)val;
        data[ofs    ] ^= tmp;
        data[ofs + 1] ^= tmp >>> 8;
        data[ofs + 2] ^= tmp >>> 16;
        data[ofs + 3] ^= tmp >>> 24;

        tmp = (int)(val >>> 32);
        data[ofs + 4] ^= tmp;
        data[ofs + 5] ^= tmp >>> 8;
        data[ofs + 6] ^= tmp >>> 16;
        data[ofs + 7] ^= tmp >>> 24;
    }

    public static boolean arraysEquals(char[] a, char[] b) {
        if (a.length != b.length) {
            return false;
        }
        return arraysEquals(a, 0, b, 0, a.length);
    }

    public static boolean arraysEquals(
            char[] a, int ofsA, char[] b, int ofsB, int len) {
        int end = ofsA + len;
        while (ofsA < end) {
            if (b[ofsB++] != a[ofsA++]) {
                return false;
            }
        }
        return true;
    }

    public static boolean arraysEquals(int[] a, int[] b) {
        if (a.length != b.length) {
            return false;
        }
        return arraysEquals(a, 0, b, 0, a.length);
    }

    public static boolean arraysEquals(
            int[] a, int ofsA, int[] b, int ofsB, int len) {
        int end = ofsA + len;
        while (ofsA < end) {
            if (b[ofsB++] != a[ofsA++]) {
                return false;
            }
        }
        return true;
    }

    public static abstract class IntRW {
        public abstract void writeInt16(short value, byte[] data, int ofs);
        public abstract void writeInt32(int   value, byte[] data, int ofs);
        public abstract void writeInt64(long  value, byte[] data, int ofs);
        public abstract short readInt16(byte[] data, int ofs);
        public abstract int   readInt32(byte[] data, int ofs);
        public abstract long  readInt64(byte[] data, int ofs);

        IntRW() { super(); }

        public final static IntRW LE = new IntRW() {
            public void  writeInt16(short value, byte[] data, int ofs) { writeInt16LE(value, data, ofs); }
            public void  writeInt32(int   value, byte[] data, int ofs) { writeInt32LE(value, data, ofs); }
            public void  writeInt64(long  value, byte[] data, int ofs) { writeInt64LE(value, data, ofs); }
            public short readInt16(byte[] data, int ofs) { return readInt16LE(data, ofs); }
            public int   readInt32(byte[] data, int ofs) { return readInt32LE(data, ofs); }
            public long  readInt64(byte[] data, int ofs) { return readInt64LE(data, ofs); }
        };

        public final static IntRW BE = new IntRW() {
            public void  writeInt16(short value, byte[] data, int ofs) { writeInt16BE(value, data, ofs); }
            public void  writeInt32(int   value, byte[] data, int ofs) { writeInt32BE(value, data, ofs); }
            public void  writeInt64(long  value, byte[] data, int ofs) { writeInt64BE(value, data, ofs); }
            public short readInt16(byte[] data, int ofs) { return readInt16BE(data, ofs); }
            public int   readInt32(byte[] data, int ofs) { return readInt32BE(data, ofs); }
            public long  readInt64(byte[] data, int ofs) { return readInt64BE(data, ofs); }
        };

        public final static IntRW instance(boolean littleEndian) {
            return littleEndian ? LE : BE;
        }

        public int[] readInt32Array(byte[] data, int ofs, int len) {
            int[] result = new int[len];
            for (int i = 0; i < len; i++) {
                result[i] = readInt32(data, ofs);
                ofs += 4;
            }
            return result;
        }

        public void writeInt32Array(int[] arr, byte[] data, int ofs) {
            for (int n : arr) {
                writeInt32(n, data, ofs);
                ofs += 4;
            }
        }
    }

    final static char[] BASE64TAB = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    static class BASE64OutputStream extends OutputStream {
        OutputStream os;
        byte[] buf = new byte[3];
        int pos;
        boolean close;
        public BASE64OutputStream(OutputStream os, boolean close) {
            this.os = os;
            this.close = close;
        }
        public void write(int b) throws IOException {
            byte[] buf = this.buf;
            buf[this.pos++] = (byte)b;
            if (3 == this.pos) {
                this.os.write(BASE64TAB[  (buf[0] >> 2)                            & 0x03f]);
                this.os.write(BASE64TAB[(((buf[0] << 4) | ((buf[1] >> 4) & 0x0f))) & 0x03f]);
                this.os.write(BASE64TAB[(((buf[1] << 2) | ((buf[2] >> 6) & 0x03))) & 0x03f]);
                this.os.write(BASE64TAB[   buf[2]                                  & 0x03f]);
                this.pos = 0;
            }
        }
        public void close() throws IOException {
            byte[] buf = this.buf;
            if (1 == this.pos) {
                this.os.write(BASE64TAB[(buf[0] >> 2) & 0x03f]);
                this.os.write(BASE64TAB[(buf[0] << 4) & 0x03f]);
                this.os.write('=');
                this.os.write('=');
            }
            else if (2 == this.pos) {
                this.os.write(BASE64TAB[  (buf[0] >> 2)                            & 0x03f]);
                this.os.write(BASE64TAB[(((buf[0] << 4) | ((buf[1] >> 4) & 0x0f))) & 0x03f]);
                this.os.write(BASE64TAB[  (buf[1] << 2)                            & 0x03f]);
                this.os.write('=');
            }
            if (this.close) {
                this.os.close();
            }
        }
    }

    static class BASE64InputStream extends InputStream {
        InputStream is;
        byte[] buf = new byte[3];
        int pos, left;
        boolean close, eos;
        public BASE64InputStream(InputStream os, boolean close) {
            this.is = os;
            this.close = close;
        }
        public int read() throws IOException {
            if (0 == this.left) {
                if (this.eos) {
                    return -1;
                }

                int c0, c1, c2, c3;
                c1 = c2 = c3 = -1;

                c0 = this.is.read();
                if (-1 == c0) {
                    this.eos = true;
                    return -1;
                }
                else if (-1 != (c1 = this.is.read()))
                     if (-1 != (c2 = this.is.read()))
                                c3 = this.is.read();

                if (-1 == c3) {
                    throw new IOException("truncated BASE64 data");
                }

                this.left = 3;
                this.pos = 0;

                if ('=' == c3) {
                    this.eos = true;
                    this.left--;
                    c3 = 'A';
                    if ('=' == c2) {
                        this.left--;
                        c2 = 'A';
                    }
                }

                c0 = inv(c0);
                c1 = inv(c1);
                c2 = inv(c2);
                c3 = inv(c3);

                if (-1 == c0 || -1 == c1 || -1 == c2 || -1 == c3) {
                    throw new IOException("invalid BASE64 character detected");
                }

                this.buf[0] = (byte)(c0 << 2 | c1 >> 4);
                this.buf[1] = (byte)(c1 << 4 | c2 >> 2);
                this.buf[2] = (byte)(c2 << 6 | c3);
            }

            this.left--;
            return this.buf[this.pos++] & 0xff;
        }
        public final static int inv(int c) {
                 if (c >= 'A' && c <= 'Z') return c - 'A';
            else if (c >= 'a' && c <= 'z') return c - 'a' + 26;
            else if (c >= '0' && c <= '9') return c - '0' + 52;
            else if (c == '+') return 62;
            else if (c == '/') return 63;
            else {
                return -1;
            }
        }
        public void close() throws IOException {
            if (this.close) {
                this.is.close();
            }
        }
    }

    public static OutputStream base64Encode(OutputStream os, boolean close) {
        return new BASE64OutputStream(os, close);
    }

    public static InputStream base64Decode(InputStream is, boolean close) {
        return new BASE64InputStream(is, close);
    }

    public static String base64Encode(byte[] data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream os = base64Encode(baos, true);
        try {
            os.write(data);
            os.close();
        }
        catch (IOException ioe) {
            return null;
        }
        return new String(baos.toByteArray());
    }

    public static byte[] base64Decode(String s) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = base64Decode(new ByteArrayInputStream(s.getBytes()), true);
        try {
            for (;;) {
                int c = is.read();
                if (-1 == c) {
                    break;
                }
                baos.write(c);
            }
            is.close();
        }
        catch (IOException ioe) {
            return null;
        }
        return baos.toByteArray();
    }

    public static String intArrayToString(int[] arr) {
        StringBuilder result = new StringBuilder();
        for (int a : arr) {
            if (0 < result.length()) {
                result.append(',');
            }
            result.append(a);
        }
        return result.toString();
    }

    public static int[] stringToIntArray(String expr) throws NumberFormatException {
        if (0 == (expr = expr.trim()).length()) {
            return new int[0];
        }
        ArrayList<Integer> lst = new ArrayList<>();
        int lastCol = -1;
        for (int pos = 0, len = expr.length(); pos < len; pos++) {
            char c = expr.charAt(pos);
            if (',' == c) {
                lst.add(Integer.decode(expr.substring(lastCol + 1, pos).trim()));
                lastCol = pos;
            }
        }
        lst.add(Integer.decode(expr.substring(lastCol + 1).trim()));
        int sz =  lst.size();
        int[] result = new int[sz];
        for (int i = 0; i < sz; i++) {
            result[i] = lst.get(i).intValue();
        }
        return result;
    }

    public static boolean checkFillValue(byte[] buf, int ofs, int len, byte val) {
        for (int end = ofs + len; ofs < end; ofs++) {
            if (buf[ofs] != val) {
                return false;
            }
        }
        return true;
    }

    public static long u32ToLng(int value) {
        return value & 0x0ffffffffL;
    }

    public static int u16ToInt(short value) {
        return value & 0x0ffff;
    }

    public static int u8ToInt(byte value) {
        return value & 0x0ff;
    }

    public static boolean flags(int value, int flags) {
        return (value & flags) == flags;
    }

    final static char[] HEXTAB = "0123456789abcdef".toCharArray();

    public static int hexDump(byte[] data, PrintStream ps,
            int bytesPerLine, int bytesPerGroup) {
        return hexDump(data, 0, data.length, ps, bytesPerLine, bytesPerGroup);
    }

    public static int hexDump(byte[] data, int ofs, int len, PrintStream ps,
            int bytesPerLine, int bytesPerGroup) {
        return hexDump(new ByteArrayInputStream(data, ofs, len), ps, -1,
                bytesPerLine, bytesPerGroup);
    }

    public static int hexDump(BytePtr bp, PrintStream ps,
            int bytesPerLine, int bytesPerGroup) {
        return hexDump(new ByteArrayInputStream(bp.buf, bp.ofs, bp.len), ps, -1,
                bytesPerLine, bytesPerGroup);
    }

    public static int hexDump(
            InputStream is, PrintStream ps,
            int maxRead, int bytesPerLine, int bytesPerGroup) {
        if (1 > bytesPerLine) {
            bytesPerLine = 16;
        }
        if (1 > bytesPerGroup) {
            bytesPerGroup = 4;
        }

        StringBuilder left  = new StringBuilder();
        StringBuilder right = new StringBuilder();

        int i = 0, result = 0;

        final String SEPARATOR = "    ";

        for (int read = 0;;) {
            if (-1 != maxRead) {
                if (maxRead <= read) {
                    break;
                }
            }

            int chr;
            try {
                if (-1 == (chr = is.read())) {
                    break;
                }
            }
            catch (IOException ioe) {
                break;
            }

            result++;

            if (0 < i && 0 == (i % bytesPerGroup)) {
                left.append(' ');
            }
            i++;

            left.append(HEXTAB[chr >>> 4]);
            left.append(HEXTAB[chr & 0x0f]);

            right.append((chr < ' ' || chr > 127) ? '.' : (char)chr);

            if (0 == (i % bytesPerLine)) {
                ps.print(left.toString());
                ps.print(SEPARATOR);
                ps.println(right.toString());

                left.setLength(0);
                right.setLength(0);

                i = 0;
            }
        }

        if (0 < i) {
            for (; i < bytesPerLine; i++) {
                left.append(' ').append(' ');
                if (0 == i % bytesPerGroup) {
                    left.append(' ');
                }
            }
            ps.print(left.toString());
            ps.print(SEPARATOR);
            ps.println(right.toString());
        }

        return result;
    }

    public static boolean bitsSet(int value, int bits) {
        return (bits & value) == bits;
    }
}
