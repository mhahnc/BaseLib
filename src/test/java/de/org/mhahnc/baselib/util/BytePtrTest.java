package de.org.mhahnc.baselib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BytePtrTest {

    @Test
    public void test0() {
        final byte[] ARR = new byte[5];
        for (int i = 0; i < ARR.length; i++) {
            ARR[i] = (byte)i;
        }

        BytePtr bp = new BytePtr(ARR);
        assertTrue(0 != bp.hashCode());
        assertTrue(bp.isValid());
        assertTrue(bp.end() == 5);
        assertTrue(bp.toString().contains("len=5"));

        for (int i = 0; i < ARR.length; i++) {
            assertTrue(bp.at(i) == i);
        }

        byte[] cpy = new byte[1 + bp.len + 1];
        cpy[0] = cpy[cpy.length - 1] = (byte)0xcc;
        bp.write(cpy, 1);
        assertTrue(0xcc == (255 & cpy[0]));
        assertTrue(0xcc == (255 & cpy[cpy.length - 1]));
        for (int i = 1; i < cpy.length - 1; i++) {
            assertTrue(cpy[i] == i - 1);
        }

        assertTrue(bp.buf == ARR);
        assertTrue(bp.ofs == 0);
        assertTrue(bp.len == ARR.length);

        assertTrue(bp == bp.grab());
        assertTrue(bp.buf != ARR);
        assertTrue(bp.ofs == 0);
        assertTrue(bp.len == ARR.length);

        for (int i = 0; i < ARR.length; i++) {
            assertTrue(bp.at(i) == i);
        }

        byte[] arr2 = bp.extract();
        assertTrue(arr2 != bp.buf);

        assertTrue(bp == bp.grab());

        BinUtils.arraysEquals(arr2, bp.buf);

        bp.buf = null;
        assertTrue(bp == bp.grab());
        assertNull(bp.buf);

        bp = new BytePtr(ARR, 0 , ARR.length + 1); assertTrue(!bp.isValid());
        bp = new BytePtr(ARR, -1, ARR.length    ); assertTrue(!bp.isValid());
        bp = new BytePtr(ARR, 1 , ARR.length    ); assertTrue(!bp.isValid());

        bp = new BytePtr(ARR, 1 , 2);
        assertTrue(bp.end() == 3);
        assertTrue(bp.isValid());
        assertTrue(bp.at(0) == 1);
        assertTrue(bp.at(1) == 2);

        arr2 = bp.extract();
        assertTrue(arr2[0] == 1);
        assertTrue(arr2[1] == 2);

        bp.grab();
        assertTrue(bp.buf[0] == 1);
        assertTrue(bp.buf[1] == 2);

        byte[] arr = ARR.clone();
        bp = new BytePtr(arr, 2 , 2);
        bp.clear();
        assertTrue(bp.at(0) == 0);
        assertTrue(bp.at(1) == 0);
        assertTrue(arr[0] == 0);
        assertTrue(arr[1] == 1);
        assertTrue(arr[2] == 0);
        assertTrue(arr[3] == 0);
        assertTrue(arr[4] == 4);

        BytePtr bp2 = new BytePtr(bp.buf, bp.ofs, bp.len);
        assertEquals(bp, bp2);
        bp2.len--;
        assertNotSame(bp, bp2);
        bp2.len++;
        assertEquals(bp, bp2);
        bp2.ofs++;
        assertNotSame(bp, bp2);
        assertNotSame(bp, "bp2");

        assertTrue (new BytePtr(new byte[10], 10, 0).isValid());
        assertTrue (new BytePtr(new byte[10],  0, 0).isValid());
        assertFalse(new BytePtr(new byte[10], 5, Integer.MAX_VALUE).isValid());
    }

    @Test
    public void testChecked() {

        byte[] buf = new byte[3];
        assertTrue(3 == new BytePtr.Checked(buf, 0, 3).len);
        assertTrue(1 == new BytePtr.Checked(buf, 1, 2).ofs);
        for (int[] p : new int[][] { { 0, 4 }, { 4, 1 }, { 10, 10 }}) {
            try {
                new BytePtr.Checked(buf, p[0], p[1]);
            }
            catch (IllegalArgumentException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }
}
