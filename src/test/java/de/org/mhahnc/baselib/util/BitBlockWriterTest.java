package de.org.mhahnc.baselib.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import org.junit.Test;

public class BitBlockWriterTest {
    @Test
    public void test0() throws Exception {
        final byte[] FILLB = { 0, (byte)255 };

        for (int fillbit = 0; fillbit < 2; fillbit++) {
            final byte[] block = new byte[10];

            final byte fb = FILLB[fillbit];

            BitBlockWriter bbw = new BitBlockWriter(block, 0, 0, blk -> {
                assertTrue(blk == block);
                for (byte b : blk) {
                    assertTrue(fb == b);
                }
            });

            bbw.flush(fillbit);
        }

    }

    void checkArray(String must, byte[] is) {
        assertTrue(BinUtils.arraysEquals(BinUtils.hexStrToBytes(must), is));
    }

    @Test
    public void test1() throws Exception {
        final byte[] block = new byte[3];

        final VarRef<Integer> c = new VarRef<>(0);

        block[0] = (byte)0xac;
        block[1] = (byte)0x5d;

        BitBlockWriter bbw = new BitBlockWriter(block, 1, 4, blk -> {
           switch(c.v++) {
               case 0: checkArray("ac3d8d", blk); break;
               case 1: checkArray("0ff087", blk); break;
               case 2: checkArray("010000", blk); break;
               default: fail();
           }
        });

        bbw.write(1, 2L);    // 0011 = 0x?3
        bbw.write(0, 2L);

        bbw.write(1, 1L);    // 10001101 = 0x8d
        bbw.write(0, 1L);
        bbw.write(1, 2L);
        bbw.write(0, 2L);
        bbw.write(0, 1L);
        bbw.write(1, 1L);

        bbw.write(1, 4L);
        bbw.write(0, 8L);    // 0x0f
        bbw.write(1, 4L);    // 0xf0

        bbw.write(1, 3L);    // [10000111][???????1]
        bbw.write(0, 4L);    // 0x87
        bbw.write(1, 2L);

        bbw.flush(0);
    }

    @Test
    public void test2() throws Exception {
        Random rnd = new Random(0xbaadbeef);

        final byte[] block = new byte[5];

        byte[] buf = new byte[8];
        final int total = block.length * buf.length * 4321;

        final ByteArrayOutputStream baos0 = new ByteArrayOutputStream(total);
        final ByteArrayOutputStream baos1 = new ByteArrayOutputStream(total);

        BitBlockWriter bbw = new BitBlockWriter(block, 0, 0, blk -> {
            try {
                baos1.write(blk);
            }
            catch (IOException ioe) {
                fail();
            }
        });

        int maxLen = 0;
        for (int i = 0; i < total; i++) {
            long r = rnd.nextLong();
            for (int j = 0; j < 64;) {
                int bit = (int)(r >> (63 - j)) & 1;
                int len = 1;
                while (++j < 64) {
                    int bit2 = (int)(r >> (63 - j)) & 1;
                    if (bit2 != bit) {
                        break;
                    }
                    len++;
                }
                maxLen = Math.max(maxLen, len);
                bbw.write(bit, len);
            }
            BinUtils.writeInt64BE(r, buf, 0);
            baos0.write(buf);
        }
        bbw.flush(0);

        // NOTE: a different implementation of Random could make this fail
        assertTrue(25 == maxLen);

        byte[] a0 = baos0.toByteArray();
        byte[] a1 = baos1.toByteArray();

        BinUtils.arraysEquals(a0, a1);
    }
}
