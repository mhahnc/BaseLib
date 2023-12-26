package de.org.mhahnc.baselib.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.org.mhahnc.baselib.test.util.FileNameMaker;

public class BlockDeviceWriterTest extends BlockDeviceWriterTestBase {

    @Test
    public void testFileNameMakers() {
        final int[] NBYTESS = new int[] {
                0, 1, 2, 3, 7, 8, 9, 100, 1000, 65537 };

        int j = 0;
        for (int i = 0; i < 2; i++) {
            FileNameMaker fnm = 0 == i ? new FileNameMaker.RandomASCII() :
                                         new FileNameMaker.RandomUnicode();

            for (int nbytes : NBYTESS) {
                j++;
                String fn = fnm.make(nbytes);

                assertTrue((0 == i ? nbytes : (nbytes >> 1)) == fn.length());

                for (char c : fn.toCharArray()) {
                    assertFalse(FileNameMaker.isResvChar(c, false));
                    if (0 == i) {
                        assertTrue((0xffffff80 & c) == 0);
                    }
                    else {
                        int ci = c & 0x0ffff;
                        assertTrue(' ' <= ci && ci < 0x0fff0);
                    }
                }
            }
        }
        assertTrue(j == NBYTESS.length * 2);
    }
}
