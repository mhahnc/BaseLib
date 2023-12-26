package de.org.mhahnc.baselib.util;

import java.io.IOException;
import java.util.Arrays;

public class BitBlockWriter {
    @FunctionalInterface
    public interface Callback {
        void onBlock(byte[] block) throws IOException;
    }

    public BitBlockWriter(byte[] block, int pos, int bits, Callback cb) {
        this.cb    = cb;
        this.block = block;
        this.pos   = pos;
        this.bits  = bits;
        this.reg   = (block[pos] << (8 - bits)) & 0x0ff;
    }

    final Callback cb;
    final byte[] block;
    int pos, reg, bits;

    public void write(int bit, long count) throws IOException {
        // cache everything on the stack for speed
        int reg = this.reg;
        int pos = this.pos;
        int bits = this.bits;
        final byte[] block = this.block;
        final int blen = block.length;

        bit &= 1;
        final int hbit = bit << 7;

        // try to fill the register
        int left = 8 - bits;
        int c = left > count ? (int)count : left;
        for (int i = 0; i < c; i++) {
            reg = (reg >>> 1) | hbit;
        }
        count -= c;
        bits += c;
        if (8 == bits) {
            block[pos++] = (byte)reg;
            bits = 0;
            if (pos == block.length) {
                this.cb.onBlock(block);
                pos = 0;
            }
        }

        // now work with bytes
        long bcount = count >> 3;
        count &= 7;

        if (0 < bcount) {
            final byte fill = (byte)(-1 + (bit ^ 1));

            // try to fill the block
            block[pos] = (byte)reg;

            left = blen - pos;
            c = left > bcount ? (int)bcount : left;

            Arrays.fill(block, pos, pos + c, fill);
            pos += c;

            bcount -= c;
            if (c == left) {
                this.cb.onBlock(block);

                // try to emit whole blocks
                long wcount = bcount / blen;
                if (0 < wcount) {
                    Arrays.fill(block, fill);
                    for (long j = 0; j < wcount; j++) {
                        this.cb.onBlock(block);
                    }
                    bcount -= wcount * blen;
                }

                // fill the rest of the bytes to the last block
                pos = (int)bcount;
                Arrays.fill(block, 0, pos, fill);
            }
        }

        // back on the bit level
        for (int i = 0; i < count; i++) {
            reg = (reg >>> 1) | hbit;
        }
        bits += (int)count;

        //System.out.printf("reg=0x%08x, pos=%d, bits=%d\n", reg, pos, bits);

        // write back the state
        this.reg = reg;
        this.pos = pos;
        this.bits = bits;
    }

    public void flush(int fillbit) throws IOException {
        // cache for speed, again
        int reg = this.reg;
        int pos = this.pos;
        int bits = this.bits;
        byte[] block = this.block;

        fillbit &= 1;
        final int hfillbit = fillbit << 7;

        // complete the register
        if (0 < bits) {
            for (; bits < 8; bits++) {
                reg = (reg >>> 1) | hfillbit;
            }
            block[pos++] = (byte)reg;
        }

        // fill the rest of the block
        final byte fill = (byte)(-1 + (fillbit ^ 1));
        for (; pos < block.length; pos++) {
            block[pos] = fill;
        }

        this.cb.onBlock(block);

        // reset the state
        this.bits = 0;
        this.pos = 0;
    }
}
