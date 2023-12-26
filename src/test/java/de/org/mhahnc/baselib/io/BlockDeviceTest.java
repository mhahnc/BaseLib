package de.org.mhahnc.baselib.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import org.junit.Test;

import de.org.mhahnc.baselib.test.util.TestUtils;
import de.org.mhahnc.baselib.util.BinUtils;
import de.org.mhahnc.baselib.util.BytePtr;

public class BlockDeviceTest {
    @Test
    public void test0() throws IOException {
        for (int type = 0; type < 2; type++) {
            final int NUM_OF_BLOCKS = 37;

            BlockDevice bd = null;
            File fl = null;
            RandomAccessFile raf = null;

            try {
                switch(type) {
                    case 0: {
                        bd = new BlockDeviceImpl.MemoryBlockDevice(
                                BlockDeviceImpl.DEFAULT_BLOCKSIZE, NUM_OF_BLOCKS, false, false);
                        break;
                    }
                    case 1: {
                        fl = new File(System.getProperty("java.io.tmpdir"),
                                      "BlockDeviceTest_test0");

                        raf = new RandomAccessFile(fl, "rw");
                        raf.setLength(NUM_OF_BLOCKS * BlockDeviceImpl.DEFAULT_BLOCKSIZE);

                        bd = new BlockDeviceImpl.FileBlockDevice(raf,
                                BlockDeviceImpl.DEFAULT_BLOCKSIZE, -1L, false, false);
                        break;
                    }
                    default: {
                        fail();
                    }
                }

                assertTrue(512 == bd.blockSize());
                assertTrue(NUM_OF_BLOCKS == bd.size());
                assertFalse(bd.readOnly());
                assertFalse(bd.writeOnly());
                assertFalse(bd.serialWrite());

                // read and write first block, check for proper offset handling
                byte[] block = new byte[1 + bd.blockSize()];
                block[0] = (byte)0xcc;
                TestUtils.fillPattern123(block, 1, block.length - 1);
                bd.write(0, block, 1);
                TestUtils.checkPattern123(block, 1, block.length - 1);
                assertTrue((byte)0xcc == block[0]);
                Arrays.fill(block, (byte)0xee);
                bd.read(0, block, 1);
                TestUtils.checkPattern123(block, 1, block.length - 1);
                assertTrue((byte)0xee == block[0]);

                // read and write through the whole block device
                block[block.length - 1] = (byte)0xaa;
                for (long i = 0; i < NUM_OF_BLOCKS; i++) {
                    Arrays.fill(block, 0, block.length - 1, (byte)i);
                    bd.write(i, block, 0);
                    assertTrue(block[block.length - 1] == (byte)0xaa);
                }
                for (long i = 0; i < NUM_OF_BLOCKS; i++) {
                    Arrays.fill(block, 0, block.length - 1, (byte)~i);
                    bd.read(i, block, 0);
                    assertTrue(block[block.length - 1] == (byte)0xaa);
                    BinUtils.checkFillValue(block, 0, block.length - 1, (byte)i);
                }
            }
            finally {
                if (null != raf) raf.close();
                if (null != fl) assertTrue(fl.delete());
            }
        }
    }

    @Test
    public void testSerialWriting() throws IOException {
        byte[] block = new byte[10];

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BlockDeviceImpl bd = new BlockDeviceImpl.OutputStreamBlockDevice(
                baos,
                5L,
                block.length,
                false);

        try {
            bd.read(0, null, 0);
            fail();
        }
        catch (IOException ioe) {
        }

        try {
            bd.write(-1, block, 0);
            fail();
        }
        catch (IOException ioe) {
        }

        TestUtils.fillPattern123(block);

        bd.write(0, block, 0);
        bd.write(1, block, 0);
        bd.write(2, block, 0);

        try {
            bd.write(4, block, 0);
            fail();
        }
        catch (IOException ioe) {
        }

        bd.write(3, block, 0);
        bd.write(4, block, 0);

        try {
            bd.write(5, block, 0);
            fail();
        }
        catch (IOException ioe) {
        }

        baos.close();
        byte[] vol = baos.toByteArray();

        assertTrue(50 == vol.length);

        for (int ofs = 0; ofs < 50; ofs += 10) {
            assertTrue(TestUtils.checkPattern123(new BytePtr(vol, ofs, 10)));
        }
    }
}
