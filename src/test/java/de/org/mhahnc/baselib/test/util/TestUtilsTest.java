package de.org.mhahnc.baselib.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.junit.Test;

import de.org.mhahnc.baselib.util.BytePtr;

public class TestUtilsTest {
    @Test
    public void testPattern123() {
        int d = 0;
        final int[] VALUES = new int[] { 0, 1, 254, 255, 256, 1023, 1024, 1025};
        for (int len : VALUES) {
            final BytePtr bp = TestUtils.fillPattern123(len);
            assertTrue(TestUtils.checkPattern123(bp));

            final byte[] buf = new byte[1 + len + 1];
            buf[0]       = (byte)0xcc;
            buf[len + 1] = (byte)0xdd;
            bp.write(buf, 1);
            assertTrue(buf[0      ] == (byte)0xcc);
            assertTrue(buf[len + 1] == (byte)0xdd);
            assertTrue(TestUtils.checkPattern123(buf, 1, len));
            for (int i = 1; i < len + 1; i++) {
                assertTrue(buf[i] == (byte)(i - 1));
            }

            if (len > 0) {
                d++;
                bp.buf[bp.ofs] = 1;
                assertFalse(TestUtils.checkPattern123(bp));
                bp.buf[bp.ofs] = 0;
                assertTrue(TestUtils.checkPattern123(bp));
                bp.buf[bp.ofs + len - 1] = (byte)(~bp.at(len - 1));
                assertFalse(TestUtils.checkPattern123(bp));
            }
        }
        assertTrue(d == VALUES.length - 1);
    }

    @Test
    public void testFile123() throws IOException {
        for (int len : new int[] { 0, 1, 254, 255, 256, 1025 }) {
            File fl = new File("testFile123.dat");

            assertTrue(!fl.exists() || fl.delete());

            TestUtils.fillFile123(fl, len);
            assertTrue(len == fl.length());
            assertTrue(len == TestUtils.checkFile123(fl));

            FileInputStream fis = new FileInputStream(fl);
            for (int i = 0; i < len; i++) {
                assertTrue((i & 0xff) == fis.read());
            }
            assertTrue(-1 == fis.read());
            fis.close();

            if (len > 0) {
                RandomAccessFile raf = new RandomAccessFile(fl, "rw");
                raf.seek(0);
                raf.write(1);
                raf.close();
                assertTrue(-1L == TestUtils.checkFile123(fl));
            }

            assertTrue(!fl.exists() || fl.delete());
        }
    }

    @Test
    public void testExtractRelativePath() throws IOException {
        File tmpDir = TestUtils.createTempDir("testutilstest_extractrelativepath");
        final String EXPECTED = "abc" + File.separatorChar + "xyz.txt";
        File fl = new File(tmpDir, EXPECTED);
        assertEquals(EXPECTED, TestUtils.extractRelativePath(tmpDir, fl));
        try {
            TestUtils.extractRelativePath(new File(tmpDir, "different"), fl);
        }
        catch (TestError expected) {
            assertTrue(-1 != expected.getMessage().indexOf(fl.getAbsolutePath()));
            assertTrue(-1 != expected.getMessage().indexOf("different"));
        }
        assertTrue(tmpDir.delete());
    }

    @Test
    public void testAreFilesEqual() throws IOException {
        File fl0 = TestUtils.createTempFile("testutilstest_arefilesequal", 10000);
        File fl1 = TestUtils.createTempFile("testutilstest_arefilesequal", 10000);
        File fl2 = TestUtils.createTempFile("testutilstest_arefilesequal", 10000);
        File fl3 = TestUtils.createTempFile("testutilstest_arefilesequal", 999);

        RandomAccessFile raf = new RandomAccessFile(fl2, "rw");
        raf.seek(5005L);
        raf.write(0);
        raf.seek(10000);
        raf.close();
        fl2 = new File(fl2.getAbsolutePath());
        assertTrue(fl2.length() == 10000L);

        assertTrue (TestUtils.areFilesEqual(fl0, fl1));
        assertFalse(TestUtils.areFilesEqual(fl0, fl2));
        assertFalse(TestUtils.areFilesEqual(fl0, fl3));
        assertFalse(TestUtils.areFilesEqual(fl2, fl3));

        assertTrue(fl3.delete());
        assertTrue(fl2.delete());
        assertTrue(fl1.delete());
        assertTrue(fl0.delete());
    }
}
