package de.org.mhahnc.baselib.io;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Block device layer which allows processing of block data in an ordered, but
 * multi-threaded fashion. Only write operations are multiplex at this moment.
 */
public final class MultiplexBlockDevice implements BlockDevice {
    final Buffer[] bufs;

    BlockDevice bdev;

    int idx;

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Default ctor.
     * @param bffact The filter factory to use. The filter actually does the
     * job of processing.
     * @param bdev The block device to read or write to.
     * @param bufCount Number of buffers to use. This is usually the number of
     * processors as reported by the runtime, but depending on the whole system
     * can be more or less (down to 1).
     * @param bufSize The number of blocks to buffer before being passed to an
     * internal thread. This gives a better performance by doing bulk processing
     * and having less time to spend on switching.
     * @throws IOException If any error occurred.
     */
    public MultiplexBlockDevice(BlockDevice.Filter.Factory bffact,
                                BlockDevice bdev,
                                int bufCount,
                                int bufSize) throws IOException {
        bffact.initialize(bdev.blockSize());

        this.bdev = bdev;

        bufCount = Math.max(1, bufCount);
        this.bufs = new Buffer[bufCount];
        for (int i = 0; i < bufCount; i++) {
            (this.bufs[i] = new Buffer(bufSize, i, bffact)).start();
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /** @see de.org.mhahnc.baselib.io.BlockDevice#blockSize() */
    public int blockSize() {
        return this.bdev.blockSize();
    }

    /** @see de.org.mhahnc.baselib.io.BlockDevice#readOnly() */
    public boolean readOnly() {
        return this.bdev.readOnly();
    }

    /** @see de.org.mhahnc.baselib.io.BlockDevice#serialWrite() */
    public boolean serialWrite() {
        return this.bdev.serialWrite();
    }

    /** @see de.org.mhahnc.baselib.io.BlockDevice#size() */
    public long size() {
        return this.bdev.size();
    }

    /** @see de.org.mhahnc.baselib.io.BlockDevice#writeOnly() */
    public boolean writeOnly() {
        return this.bdev.writeOnly();
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean __TEST_sync_process;
    public static Integer __TEST_run_delay;

    class Buffer extends Thread {
        final int sz;
        final int bsz = MultiplexBlockDevice.this.bdev.blockSize();

        final BlockDevice.Filter fread;
        final BlockDevice.Filter fwrite;

        final byte[] buf;
        final long[] nums;

        IOException ioerr;
        int         pos;

        AtomicInteger busy = new AtomicInteger();

        public Buffer(int sz, int num,
                      BlockDevice.Filter.Factory bffact) {
            super("MBD.Buffer" + num);

            this.fwrite = MultiplexBlockDevice.this.bdev.readOnly () ? null : bffact.createWrite();
            this.fread  = MultiplexBlockDevice.this.bdev.writeOnly() ? null : bffact.createRead();

            this.sz = sz;

            this.buf  = new byte[sz * this.bsz];
            this.nums = new long[sz];
        }

        public void end() {
            synchronized(this.buf) {
                this.busy.set(-1);
                this.buf.notify();
            }
        }

        public void run() {
            for (;;) {
                if (0 == this.busy.get()) {
                    synchronized(this.buf) {
                        while (0 == this.busy.get()) {
                            try {
                                this.buf.wait();
                            }
                            catch (InterruptedException ire) {
                                return;
                            }
                        }
                        if (-1 == this.busy.get()) {
                            return;
                        }
                    }
                }

                try {
                    for (int i = 0; i < this.pos; i++) {
                        long num = this.fwrite.map(this.nums[i]);
                        this.nums[i] = num;
                        this.fwrite.transform(
                                num,
                                this.buf,
                                this.bsz * i);

                    }
                }
                catch (IOException ioe) {
                    this.ioerr = ioe;
                }

                if (null != MultiplexBlockDevice.__TEST_run_delay) {
                    try { Thread.sleep(MultiplexBlockDevice.__TEST_run_delay); }
                    catch (InterruptedException ire) { }
                }

                synchronized(this.buf) {
                    this.buf.notify();
                    if (-1 == this.busy.get()) {
                        return;
                    }
                    this.busy.set(0);
                }
            }
        }

        void notbusy(boolean closing, boolean err) throws IOException {
            if (1 == this.busy.get()) {
                synchronized(this.buf) {
                    while(1 == this.busy.get()) {
                        try {
                            this.buf.wait();
                        }
                        catch (InterruptedException ire) {
                            throw new IOException(ire);
                        }
                    }
                }
                if (null != this.ioerr) {
                    throw this.ioerr;
                }
            }
            if (err) {
                return;
            }
            if (full() || closing) {
                flush();
            }
        }

        void process() {
            synchronized(this.buf) {
                this.busy.set(1);
                this.buf.notify();
            }
        }

        boolean full() {
            return this.pos == this.sz;
        }

        boolean add(long num, byte[] block, int ofs) throws IOException {
            notbusy(false, false);

            System.arraycopy(block,
                             ofs,
                             this.buf,
                             this.bsz * this.pos,
                             this.bsz);

            this.nums[this.pos] = num;

            ++this.pos;

            if (full()) {
                process();
                if (__TEST_sync_process) {
                    while (1 == this.busy.get()) {
                        try { Thread.sleep(1); }
                        catch (InterruptedException ignored) { }
                    }
                }
                return true;
            }
            return false;
        }

        void flush() throws IOException {
            for (int i = 0; i < this.pos; i++) {
                MultiplexBlockDevice.this.bdev.write(
                        this.nums[i],
                        this.buf,
                        i * this.bsz);
            }
            this.pos = 0;
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    private void incIdx() {
        this.idx = (this.idx + 1) % this.bufs.length;
    }

    /** @see de.org.mhahnc.baselib.io.BlockDevice#write(long, byte[], int) */
    public void write(long num, byte[] block, int ofs) throws IOException {
        if (null == this.bdev) {
            throw new IOException();
        }

        Buffer buf = this.bufs[this.idx];

        if (buf.add(num, block, ofs)) {
            incIdx();
        }
    }

    private void closeWrite(boolean err) throws IOException {
        {
            Buffer buf = this.bufs[this.idx];

            if (0 == buf.busy.get() &&
                     buf.pos > 0    &&
                     buf.pos < buf.sz) {
                buf.process();
                incIdx();
            }
        }

        for (int i = 0; i < this.bufs.length; i++) {
            Buffer buf = this.bufs[this.idx];

            buf.notbusy(true, err);

            incIdx();
        }

        for (Buffer buf : this.bufs) {
            buf.end();
        }
        for (Buffer buf : this.bufs) {
            try {
                buf.join();
            }
            catch (InterruptedException ire) {
                throw new IOException(ire);
            }
        }
    }

    /** @see de.org.mhahnc.baselib.io.BlockDevice#close(boolean) close() */
    public void close(boolean err) throws IOException {
        if (null != this.bdev) {
            closeWrite(err);

            this.bdev = null;
        }
    }

    /** @see de.org.mhahnc.baselib.io.BlockDevice#read(long, byte[], int) */
    public void read(long num, byte[] block, int ofs) throws IOException {
        // TODO: multiplex that
        num = this.bufs[0].fread.map(num);
        this.bdev.read(num, block, ofs);
        this.bufs[0].fread.transform(num, block, ofs);
    }
}
