package de.org.mhahnc.baselib.util;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class BinUtilsTest {

    @Test
    public void testBytesToHexString() {
        assertTrue(""      .equals(BinUtils.bytesToHexString(new byte[0])));
        assertTrue("00"    .equals(BinUtils.bytesToHexString(new byte[] { 0} )));
        assertTrue("0001ff".equals(BinUtils.bytesToHexString(new byte[] { 0, 1, (byte)0xff} )));
        assertTrue("01"    .equals(BinUtils.bytesToHexString(new byte[] { 0, 1 }   , 1, 1 )));
        assertTrue(""      .equals(BinUtils.bytesToHexString(new byte[] { 0, 1 }   , 1, 0 )));
        assertTrue("09"    .equals(BinUtils.bytesToHexString(new byte[] { 7, 7, 9 }, 2, 1 )));
        byte[] buf  = new byte[2];
        buf[0] = (byte)0xcc;
        for (int i = 0; i < 256; i++) {
            buf[1] = (byte)i;
            assertTrue(String.format("%02x", i & 0xff).equals(BinUtils.bytesToHexString(buf, 1, 1)));
            assertTrue(0xcc == (buf[0] & 0xff));
        }
    }

    @Test
    public void testHexStrToBytes() {
        byte[] b;

        assertTrue(null == BinUtils.hexStrToBytes("x"));
        assertTrue(null == BinUtils.hexStrToBytes("aaX"));
        assertTrue(null == BinUtils.hexStrToBytes("00a"));
        assertTrue(null == BinUtils.hexStrToBytes("123"));
        assertTrue(null == BinUtils.hexStrToBytes("(*%^()^%(&^@&%^@&#43"));

        b = BinUtils.hexStrToBytes("");
        assertTrue(0 == b.length);
        b = BinUtils.hexStrToBytes("00");
        assertTrue(1 == b.length);
        assertTrue(0 == b[0]);
        b = BinUtils.hexStrToBytes("ff");
        assertTrue(1 == b.length);
        assertTrue((byte)0xff == b[0]);
        b = BinUtils.hexStrToBytes("0123456789abcdef");
        assertTrue(8 == b.length);
        assertTrue((byte)0x01 == b[0]); assertTrue((byte)0x23 == b[1]);
        assertTrue((byte)0x45 == b[2]); assertTrue((byte)0x67 == b[3]);
        assertTrue((byte)0x89 == b[4]); assertTrue((byte)0xab == b[5]);
        assertTrue((byte)0xcd == b[6]); assertTrue((byte)0xef == b[7]);
        b = BinUtils.hexStrToBytes("BAADF00D");
        assertTrue(4 == b.length);
        assertTrue((byte)0xba == b[0]); assertTrue((byte)0xad == b[1]);
        assertTrue((byte)0xf0 == b[2]); assertTrue((byte)0x0d == b[3]);
    }

    @Test
    public void testReadWriteIntegers() {

        byte[] buf = new byte[4];
        buf[0] = buf[3] = (byte)0xcc;
        BinUtils.writeInt16BE((short)0xfedc, buf,  1);
        assertTrue(0xcc == (255 & buf[0]));
        assertTrue(0xfe == (255 & buf[1]));
        assertTrue(0xdc == (255 & buf[2]));
        assertTrue(0xcc == (255 & buf[3]));
        assertTrue(0xfedc == (0xffff & BinUtils.readInt16BE(buf, 1)));
        BinUtils.writeInt16LE((short)0xfedc, buf,  1);
        assertTrue(0xcc == (255 & buf[0]));
        assertTrue(0xfe == (255 & buf[2]));
        assertTrue(0xdc == (255 & buf[1]));
        assertTrue(0xcc == (255 & buf[3]));
        assertTrue(0xfedc == (0xffff & BinUtils.readInt16LE(buf, 1)));

        buf = new byte[6];
        buf[0] = buf[5] = (byte)0xcc;
        BinUtils.writeInt32BE(0xfedcba98, buf,  1);
        assertTrue(0xcc == (255 & buf[0]));
        assertTrue(0xfe == (255 & buf[1]));
        assertTrue(0xdc == (255 & buf[2]));
        assertTrue(0xba == (255 & buf[3]));
        assertTrue(0x98 == (255 & buf[4]));
        assertTrue(0xcc == (255 & buf[5]));
        assertTrue(0xfedcba98 == BinUtils.readInt32BE(buf, 1));
        BinUtils.writeInt32LE(0xfedcba98, buf,  1);
        assertTrue(0xcc == (255 & buf[0]));
        assertTrue(0xfe == (255 & buf[4]));
        assertTrue(0xdc == (255 & buf[3]));
        assertTrue(0xba == (255 & buf[2]));
        assertTrue(0x98 == (255 & buf[1]));
        assertTrue(0xcc == (255 & buf[5]));
        assertTrue(0xfedcba98 == BinUtils.readInt32LE(buf, 1));

        buf = new byte[10];
        buf[0] = buf[9] = (byte)0xcc;
        BinUtils.writeInt64BE(0xfedcba9876543210L, buf,  1);
        assertTrue(0xcc == (255 & buf[0]));
        assertTrue(0xfe == (255 & buf[1]));
        assertTrue(0xdc == (255 & buf[2]));
        assertTrue(0xba == (255 & buf[3]));
        assertTrue(0x98 == (255 & buf[4]));
        assertTrue(0x76 == (255 & buf[5]));
        assertTrue(0x54 == (255 & buf[6]));
        assertTrue(0x32 == (255 & buf[7]));
        assertTrue(0x10 == (255 & buf[8]));
        assertTrue(0xcc == (255 & buf[9]));
        assertTrue(0xfedcba9876543210L == BinUtils.readInt64BE(buf, 1));
        BinUtils.writeInt64LE(0xfedcba9876543210L, buf,  1);
        assertTrue(0xcc == (255 & buf[0]));
        assertTrue(0xfe == (255 & buf[8]));
        assertTrue(0xdc == (255 & buf[7]));
        assertTrue(0xba == (255 & buf[6]));
        assertTrue(0x98 == (255 & buf[5]));
        assertTrue(0x76 == (255 & buf[4]));
        assertTrue(0x54 == (255 & buf[3]));
        assertTrue(0x32 == (255 & buf[2]));
        assertTrue(0x10 == (255 & buf[1]));
        assertTrue(0xcc == (255 & buf[9]));
        assertTrue(0xfedcba9876543210L == BinUtils.readInt64LE(buf, 1));
    }

    @Test
    public void testLongsToBytesLE() {
        assertTrue(BinUtils.arraysEquals(
                new byte[] { 0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef },
                BinUtils.longsToBytesLE(new long[] { 0xefcdab8967452301L })));

        assertTrue(BinUtils.arraysEquals(
                new byte[] { 1,0,0,0,0,0,0,1,
                             (byte)0x0d, (byte)0xf0, (byte)0xad, (byte)0xba,
                             (byte)0xbe, (byte)0xba, (byte)0xfe, (byte)0xca},
                BinUtils.longsToBytesLE(new long[] { 0x0100000000000001L, 0xcafebabebaadf00dL })));

        assertTrue(BinUtils.arraysEquals(new byte[0], BinUtils.longsToBytesLE(new long[0])));
    }

    @Test
    public void testArrayEquals() {

        // byte testing
        assertTrue( BinUtils.arraysEquals(new byte[0]           , 0, new byte[0]           , 0, 0));
        assertTrue( BinUtils.arraysEquals(new byte[1]           , 0, new byte[1]           , 0, 1));
        assertTrue( BinUtils.arraysEquals(new byte[2]           , 1, new byte[2]           , 1, 1));
        assertTrue( BinUtils.arraysEquals(new byte[] { 4, 5 }   , 1, new byte[] { 3, 5 }   , 1, 1));
        assertTrue(!BinUtils.arraysEquals(new byte[] { 4, 5 }   , 0, new byte[] { 3, 5 }   , 0, 2));
        assertTrue( BinUtils.arraysEquals(new byte[] { 4, 5 }   , 1, new byte[] { 3, 5 }   , 1, 1));
        assertTrue( BinUtils.arraysEquals(new byte[] { 4, 5, 6 }, 1, new byte[] { 5, 6, 3 }, 0, 2));
        assertTrue( BinUtils.arraysEquals(new byte[] { 4, 5, 6 }, 1, new byte[] { 5, 6, 3 }, 0, 1));
        assertTrue( BinUtils.arraysEquals(new byte[] { 4, 5, 6 }, 2, new byte[] { 5, 6, 3 }, 1, 1));
        assertTrue(!BinUtils.arraysEquals(new byte[] { 4, 5, 6 }, 0, new byte[] { 5, 6, 3 }, 0, 3));

        assertTrue(!BinUtils.arraysEquals(new byte[0], new byte[1]));
        assertTrue( BinUtils.arraysEquals(new byte[0], new byte[0]));

        // char testing
        assertTrue( BinUtils.arraysEquals(new char[0]           , 0, new char[0]           , 0, 0));
        assertTrue( BinUtils.arraysEquals(new char[1]           , 0, new char[1]           , 0, 1));
        assertTrue( BinUtils.arraysEquals(new char[2]           , 1, new char[2]           , 1, 1));
        assertTrue( BinUtils.arraysEquals(new char[] { 4, 5 }   , 1, new char[] { 3, 5 }   , 1, 1));
        assertTrue(!BinUtils.arraysEquals(new char[] { 4, 5 }   , 0, new char[] { 3, 5 }   , 0, 2));
        assertTrue( BinUtils.arraysEquals(new char[] { 4, 5 }   , 1, new char[] { 3, 5 }   , 1, 1));
        assertTrue( BinUtils.arraysEquals(new char[] { 4, 5, 6 }, 1, new char[] { 5, 6, 3 }, 0, 2));
        assertTrue( BinUtils.arraysEquals(new char[] { 4, 5, 6 }, 1, new char[] { 5, 6, 3 }, 0, 1));
        assertTrue( BinUtils.arraysEquals(new char[] { 4, 5, 6 }, 2, new char[] { 5, 6, 3 }, 1, 1));
        assertTrue(!BinUtils.arraysEquals(new char[] { 4, 5, 6 }, 0, new char[] { 5, 6, 3 }, 0, 3));

        assertTrue(!BinUtils.arraysEquals(new char[0], new char[1]));
        assertTrue( BinUtils.arraysEquals(new char[0], new char[0]));

        // int testing
        assertTrue( BinUtils.arraysEquals(new int[0]           , 0, new int[0]           , 0, 0));
        assertTrue( BinUtils.arraysEquals(new int[1]           , 0, new int[1]           , 0, 1));
        assertTrue( BinUtils.arraysEquals(new int[2]           , 1, new int[2]           , 1, 1));
        assertTrue( BinUtils.arraysEquals(new int[] { 4, 5 }   , 1, new int[] { 3, 5 }   , 1, 1));
        assertTrue(!BinUtils.arraysEquals(new int[] { 4, 5 }   , 0, new int[] { 3, 5 }   , 0, 2));
        assertTrue( BinUtils.arraysEquals(new int[] { 4, 5 }   , 1, new int[] { 3, 5 }   , 1, 1));
        assertTrue( BinUtils.arraysEquals(new int[] { 4, 5, 6 }, 1, new int[] { 5, 6, 3 }, 0, 2));
        assertTrue( BinUtils.arraysEquals(new int[] { 4, 5, 6 }, 1, new int[] { 5, 6, 3 }, 0, 1));
        assertTrue( BinUtils.arraysEquals(new int[] { 4, 5, 6 }, 2, new int[] { 5, 6, 3 }, 1, 1));
        assertTrue(!BinUtils.arraysEquals(new int[] { 4, 5, 6 }, 0, new int[] { 5, 6, 3 }, 0, 3));

        assertTrue(!BinUtils.arraysEquals(new int[0], new int[1]));
        assertTrue( BinUtils.arraysEquals(new int[0], new int[0]));
    }

    @Test
    public void testIntRW() {
        final byte[] DATA = { (byte)0xcc,
                (byte)0xf0,(byte)0xe1,(byte)0xd2,(byte)0xc3,
                (byte)0xb4,(byte)0xa5,(byte)0x96,(byte)0x87 };

        BinUtils.IntRW irw = BinUtils.IntRW.instance(true);

        assertTrue((short)0xe1f0 == irw.readInt16(DATA, 1));
        assertTrue((short)0xa5b4 == irw.readInt16(DATA, 5));
        assertTrue(0xc3d2e1f0 == irw.readInt32(DATA, 1));
        assertTrue(0x8796a5b4 == irw.readInt32(DATA, 5));
        assertTrue(0x8796a5b4c3d2e1f0L == irw.readInt64(DATA, 1));

        irw = BinUtils.IntRW.instance(false);

        assertTrue((short)0xf0e1 == irw.readInt16(DATA, 1));
        assertTrue((short)0xb4a5 == irw.readInt16(DATA, 5));
        assertTrue(0xf0e1d2c3 == irw.readInt32(DATA, 1));
        assertTrue(0xb4a59687 == irw.readInt32(DATA, 5));
        assertTrue(0xf0e1d2c3b4a59687L == irw.readInt64(DATA, 1));

        byte[] buf = new byte[10];

        irw = BinUtils.IntRW.instance(true);

        Arrays.fill(buf, (byte)0x77);
        irw.writeInt16((short)0xe1f0, buf, 1);
        assertTrue(0x77 == buf[0] && 0x77 == buf[3]);
        assertTrue(BinUtils.arraysEquals(buf, 1, DATA, 1, 2));
        Arrays.fill(buf, (byte)0x77);
        irw.writeInt32(0xc3d2e1f0, buf, 1);
        assertTrue(0x77 == buf[0] && 0x77 == buf[5]);
        assertTrue(BinUtils.arraysEquals(buf, 1, DATA, 1, 4));
        Arrays.fill(buf, (byte)0x77);
        irw.writeInt64(0x8796a5b4c3d2e1f0L, buf, 1);
        assertTrue(0x77 == buf[0] && 0x77 == buf[9]);
        assertTrue(BinUtils.arraysEquals(buf, 1, DATA, 1, 8));

        irw = BinUtils.IntRW.instance(false);

        Arrays.fill(buf, (byte)0x77);
        irw.writeInt16((short)0xf0e1, buf, 1);
        assertTrue(0x77 == buf[0] && 0x77 == buf[3]);
        assertTrue(BinUtils.arraysEquals(buf, 1, DATA, 1, 2));
        Arrays.fill(buf, (byte)0x77);
        irw.writeInt32(0xf0e1d2c3, buf, 1);
        assertTrue(0x77 == buf[0] && 0x77 == buf[5]);
        assertTrue(BinUtils.arraysEquals(buf, 1, DATA, 1, 4));
        Arrays.fill(buf, (byte)0x77);
        irw.writeInt64(0xf0e1d2c3b4a59687L, buf, 1);
        assertTrue(0x77 == buf[0] && 0x77 == buf[9]);
        assertTrue(BinUtils.arraysEquals(buf, 1, DATA, 1, 8));
    }

    @Test
    public void testBASE64() {
        final byte[] WIKIPEDIA_TEST_DATA = "Man is distinguished, not only by his reason, but by this singular passion from other animals, which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable generation of knowledge, exceeds the short vehemence of any carnal pleasure.".getBytes();
        final String WIKIPEDIA_TEST_DATA_BASE64 =
            "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz" +
            "IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg" +
            "dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu" +
            "dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo" +
            "ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4=";

        String enc = BinUtils.base64Encode(WIKIPEDIA_TEST_DATA);
        assertTrue(enc.equals(WIKIPEDIA_TEST_DATA_BASE64));
        byte[] dec = BinUtils.base64Decode(enc);
        assertTrue(BinUtils.arraysEquals(WIKIPEDIA_TEST_DATA, dec));

        for (int i = 0; i < 257; i++) {
            byte[] buf = new byte[i];
            for (int j = 0; j < i; j++) {
                buf[j] = (byte)j;
            }
            assertTrue(BinUtils.arraysEquals(buf,
                    BinUtils.base64Decode(
                    BinUtils.base64Encode(buf))));
        }

        assertNull(BinUtils.base64Decode("="));
        assertNull(BinUtils.base64Decode("a==="));
        assertNull(BinUtils.base64Decode("AAA$"));
        assertNull(BinUtils.base64Decode("AAAAB=B="));
        assertNull(BinUtils.base64Decode("AAAA=BB="));
    }

    @Test
    public void testIntArrays() throws NumberFormatException {
        final int[] TEST1 = new int[] { 1, 2, 3, 117 };

        assertTrue("1,2,3,117".equals(BinUtils.intArrayToString(TEST1)));

        for (String s : new String[] {
                "1,2,3,117",
                "1, 2,3,117",
                "1,2,3,117",
                " 1,2, 0x3 ,117",
                " 1 , 2 ,   \r    3 ,  \t    117 \n"
        }) {
            assertTrue(BinUtils.arraysEquals(TEST1, BinUtils.stringToIntArray(s)));
        }

        for (int[] arr : new int[][] {
            new int[] { },
            new int[] { 0 },
            new int[] { 100000001 },
            new int[] { -42, ~(1 << 31), -1}
        }) {
            String s = BinUtils.intArrayToString(arr);
            int[] arr2 = BinUtils.stringToIntArray(s);
            assertTrue(BinUtils.arraysEquals(arr, arr2));
        }

        for (String bad : new String[] {
                "-",
                "1,abc,3",
                ",",
                "1,",
                "1,2,3,",
                "1,2,3,4,notworking",
                "1,bad,3",
                "3,4x4,5"
        }) {
            try {
                BinUtils.stringToIntArray(bad);
                fail();
            }
            catch (NumberFormatException nfe) {
            }
        }
    }

    @Test
    public void testExtends() {
        assertTrue(0L == BinUtils.u32ToLng(0));
        assertTrue(0x0ffffffffL == BinUtils.u32ToLng(-1));
        assertTrue(0 == BinUtils.u16ToInt((short)0));
        assertTrue(0x0ffffL == BinUtils.u16ToInt((short)-1));
    }
}
