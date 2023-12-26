package de.org.mhahnc.baselib.io;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Test;

import de.org.mhahnc.baselib.io.BlockDevice.Filter;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class MultiplexBlockDeviceTest {
    @After
    public void tearDown() {
        MultiplexBlockDevice.__TEST_sync_process = false;
        MultiplexBlockDevice.__TEST_run_delay    = null;
    }

    ///////////////////////////////////////////////////////////////////////////

    int bsz = -1;

    ///////////////////////////////////////////////////////////////////////////

    final static int  TOFS    = 1;
    final static byte TPREFIX = (byte)0xcc;

    byte[] makeBlock(long num) {
        byte[] result = new byte[TOFS + this.bsz];
        Arrays.fill(result, 0, TOFS, TPREFIX);
        Arrays.fill(result, TOFS, result.length, (byte)num);
        return result;
    }

    public boolean verifyBlock(byte[] buf, int ofs, long num, boolean prefixed, int inc) {
        if (prefixed) {
            for (int i = 0; i < ofs; i++) {
                if (buf[i] != TPREFIX) {
                    return false;
                }
            }
        }
        for (int i = ofs, end = ofs + this.bsz; i < end; i++) {
            if ((byte)(buf[i] - inc) != (byte)num) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testVerify() {
        for (final long num : new long[] { 1, 3, 10000, 0x0123456789abcdefL})
        for (final int  bsz : new int [] { 1, 2, 3, 4, 501, 502, 512, 100000}) {
            this.bsz = bsz;
            byte[] blk  = makeBlock(num);
            assertTrue(blk.length == bsz + 1);
            byte[] blk2 = new byte[bsz];
            System.arraycopy(blk, TOFS, blk2, 0, bsz);

            assertTrue(verifyBlock(blk , TOFS, num    , true , 0));
            assertTrue(verifyBlock(blk , TOFS, num - 1, true , 1));
            assertTrue(verifyBlock(blk2, 0   , num    , false, 0));
            assertTrue(verifyBlock(blk2, 0   , num - 1, false, 1));

            blk[0] = (byte)0xcd;   assertFalse(verifyBlock(blk, TOFS, num, true, 0));
            blk[0] = TPREFIX;      assertTrue (verifyBlock(blk, TOFS, num, true, 0));
            blk[1]--;              assertFalse(verifyBlock(blk, TOFS, num, true, 0));
            blk[1]++;              assertTrue (verifyBlock(blk, TOFS, num, true, 0));
            blk[blk.length - 1]--; assertFalse(verifyBlock(blk, TOFS, num, true, 0));
            blk[blk.length - 1]++; assertTrue (verifyBlock(blk, TOFS, num, true, 0));

            blk2[0]--;               assertFalse(verifyBlock(blk2, 0, num, false, 0));
            blk2[0]++;               assertTrue (verifyBlock(blk2, 0, num, false, 0));
            blk2[blk2.length - 1]--; assertFalse(verifyBlock(blk2, 0, num, false, 0));
            blk2[blk2.length - 1]++; assertTrue (verifyBlock(blk2, 0, num, false, 0));
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    public void dotest(int bufsz) throws Exception {
        this.bsz = bufsz;

        for (int[] cfg : new int[][] {
                {  1,  1,     1 },
                {  2,  1,     1 },
                {  1,  2,     1 },
                {  1,  1,    10 },
                {  2,  1,    11 },
                {  1,  2,    12 },
                {  2,  3,   100 },
                { 10,  5, 10117 },
                { 33, 33, 33333 }
        }) {
            IncTransFactory     itf  = new IncTransFactory(23);
            SeqWriteBlockDevice swbd = new SeqWriteBlockDevice();

            final int bufcount = cfg[0];
            final int bufsize  = cfg[1];
            final int blocks   = cfg[2];

           MultiplexBlockDevice mbd = new MultiplexBlockDevice(
                    itf,
                    swbd,
                    bufcount,
                    bufsize);

            assertFalse(mbd.readOnly());
            assertFalse(mbd.serialWrite());
            assertTrue (mbd.writeOnly());
            assertTrue (mbd.blockSize() == this.bsz);

            assertFalse(-1 == itf.blockSize);

            for (int num = 0; num < blocks; num++) {
                byte[] blk = makeBlock(num);

                mbd.write(num, blk, TOFS);

                assertTrue(verifyBlock(blk, TOFS, num, true , 0));
            }

            mbd.close(false);

            assertTrue(blocks == swbd.writes);
            assertTrue(blocks == itf.transforms.get());
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void test0() throws Exception {
        for (final int bufsz : new int[] { 1, 16, 512, 1001, 4096}) {
            dotest(bufsz);
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void testSyncProcess() throws Exception {
        MultiplexBlockDevice.__TEST_sync_process = true;

        for (final int bufsz : new int[] { 1, 512 }) {
            dotest(bufsz);
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void testRunDelay() throws Exception {
        MultiplexBlockDevice.__TEST_run_delay = 10;

        for (final int bufsz : new int[] { 512 }) {
            dotest(bufsz);
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    static class IncTransFactory implements BlockDevice.Filter.Factory {
        final static int INC = 3;

        final long noopLoops;

        public IncTransFactory(long noopLoops) {
            this.noopLoops = noopLoops;
        }

        public Filter createRead() {
            fail();
            return null;
        }

        public Filter createWrite() {
            return new Filter() {
                public void transform(
                        long num, byte[] block, int ofs) throws IOException {
                    for (int i = ofs, end = ofs + IncTransFactory.this.blockSize;
                         i < end; i++) {
                        block[i] = (byte)(block[i] + INC);
                    }
                    for (long nol = 0; nol < IncTransFactory.this.noopLoops; nol++) {
                        for (int i = ofs, end = ofs + IncTransFactory.this.blockSize;
                             i < end; i++) {
                            block[i] = (byte)(block[i] + 256);
                        }
                    }
                    IncTransFactory.this.transforms.incrementAndGet();
                }
                public long map(long num) {
                    return num;
                }
            };
        }

        public void initialize(int blockSize) throws IOException {
            this.blockSize = blockSize;
        }
        int blockSize = -1;

        AtomicInteger transforms = new AtomicInteger();
    }

    class SeqWriteBlockDevice implements BlockDevice {
        public int blockSize() {
            return MultiplexBlockDeviceTest.this.bsz;
        }

        public void close(boolean err) throws IOException {
            this.closed++;
        }
        long closed;

        public void read(long num, byte[] block, int ofs) throws IOException {
            throw new IOException();
        }

        public boolean readOnly() {
            return false;
        }

        public boolean serialWrite() {
            return false;
        }

        public long size() {
            return SIZE;
        }
        final static long SIZE = 0x123456789abcL;

        public void write(long num, byte[] block, int ofs) throws IOException {
            assertTrue(verifyBlock(block, ofs, num, false, IncTransFactory.INC));
            if (-1L == this.lastNum) {
                this.lastNum = num;
            }
            else {
                assertTrue(++this.lastNum == num);
            }
            this.writes++;
        }
        long lastNum = -1L;
        long writes;

        public boolean writeOnly() {
            return true;    // important for the filter factory to work!
        }
    }
}
