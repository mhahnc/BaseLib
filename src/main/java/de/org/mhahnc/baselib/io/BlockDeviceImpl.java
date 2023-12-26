package de.org.mhahnc.baselib.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * Block device implementations.
 */
public abstract class BlockDeviceImpl implements BlockDevice {
    public final static int DEFAULT_BLOCKSIZE = 512;

    final protected boolean readOnly;
    final protected boolean writeOnly;
    final protected boolean serialWrite;

    final protected int  blockSize;
    final protected long size;

    protected long lastNum = -1L;

    protected BlockDeviceImpl(
            boolean readOnly, boolean writeOnly, boolean serialWrite, long size, int blockSize) {
        if (readOnly && writeOnly) {
            throw new IllegalArgumentException("readonly and writeonly are mutually exclusive");
        }
        if (readOnly && serialWrite) {
            throw new IllegalArgumentException("readonly and serialwrite are mutually exclusive");
        }
        this.readOnly    = readOnly;
        this.writeOnly   = writeOnly;
        this.serialWrite = serialWrite;
        this.size        = size;
        this.blockSize   = blockSize;
    }

    public int blockSize() {
        return this.blockSize;
    }

    public boolean readOnly() {
        return this.readOnly;
    }

    public boolean writeOnly() {
        return this.writeOnly;
    }

    public boolean serialWrite() {
        return this.serialWrite;
    }

    public long size() {
        return this.size;
    }

    protected abstract void internalRead (long num, byte[] block, int ofs) throws IOException;
    protected abstract void internalWrite(long num, byte[] block, int ofs) throws IOException;

    public void read(long num, byte[] block, int ofs) throws IOException {
        if (this.writeOnly) {
            throw new IOException("block device is writeonly");
        }
        internalRead(num, block, ofs);
    }

    public void write(long num, byte[] block, int ofs) throws IOException {
        if (this.readOnly) {
            throw new IOException("block device is readonly");
        }
        if (num >= this.size ||
            num < 0L) {
            throw new IOException(String.format(
                    "illegal number (%d, size=%d)",
                    num, this.size));
        }
        if (this.serialWrite) {
            if (num != this.lastNum + 1) {
                throw new IOException(String.format(
                        "illegal seek (%d<>%d)",
                        num, this.lastNum + 1));
            }
        }
        if (null != block) {
            internalWrite(num, block, ofs);
            this.lastNum = num;
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * To map block device functionality on a random access file.
     */
    public static class FileBlockDevice extends BlockDeviceImpl {
        RandomAccessFile file;

        public FileBlockDevice(RandomAccessFile file, int blockSize, long size,
                boolean readOnly, boolean writeOnly) throws IOException {
            super(readOnly,
                  writeOnly,
                  false,
                  -1L == size ? (file.length() / blockSize) : size,
                  blockSize);

            this.file = file;
        }
        public void internalRead(long num, byte[] block, int ofs) throws IOException {
            this.file.seek(num * this.blockSize);
            if (this.blockSize != this.file.read(block, ofs, this.blockSize)) {
                throw new IOException(String.format(
                        "reading block %d from random access file failed", num));
            }
        }
        protected void internalWrite(long num, byte[] block, int ofs) throws IOException {
            this.file.seek(num * this.blockSize);
            this.file.write(block, ofs, this.blockSize);
        }
        public void close(boolean err) throws IOException {
            this.file.close();
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Block device mapped by an output stream. I/O can logically only happen
     * in a serial manner. The size of the device must be defined though.
     */
    public static class OutputStreamBlockDevice extends BlockDeviceImpl {
        OutputStream os;
        boolean doClose;

        public OutputStreamBlockDevice(
                OutputStream os, long size, int blockSize, boolean doClose) {
            super(false, true, true, size, blockSize);

            this.os      = os;
            this.doClose = doClose;
        }
        protected void internalWrite(long num, byte[] block, int ofs) throws IOException {
            if (num >= this.size) {
                throw new IOException();
            }
            this.os.write(block, ofs, blockSize());
        }
        public void internalRead(long num, byte[] block, int ofs) throws IOException {
            throw new IOException();
        }
        public void close(boolean err) throws IOException {
            if (null != this.os) {
                if (this.doClose) {
                    this.os.close();
                }
                this.os = null;
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Block device backed by memory.
     */
    public static class MemoryBlockDevice extends BlockDeviceImpl {
        byte[] buf;

        public MemoryBlockDevice(int blockSize, long size,
                boolean readOnly, boolean writeOnly) throws IOException {
            super(readOnly, writeOnly, false, size, blockSize);

            this.buf = new byte[numToOfs(size)];
        }
        public MemoryBlockDevice(int blockSize, byte[] buf,
                boolean readOnly, boolean writeOnly) throws IOException {
            super(readOnly,
                  writeOnly,
                  false,
                  buf.length / blockSize,
                  blockSize);
            if (0 != buf.length % blockSize) {
                throw new IOException("buffer not aligned to block size");
            }
            this.buf = buf;
        }
        public void internalRead(long num, byte[] block, int ofs) throws IOException {
            System.arraycopy(this.buf, numToOfs(num), block, ofs, this.blockSize);
        }
        protected void internalWrite(long num, byte[] block, int ofs) throws IOException {
            System.arraycopy(block, ofs, this.buf, numToOfs(num), this.blockSize);
        }
        public void close(boolean err) {
        }
        public byte[] buffer() {
            return this.buf;
        }
        private int numToOfs(long num) throws IOException {
            long result = num * this.blockSize;
            if ((result & 0xffffffff80000000L) != 0L) {
                throw new IOException("memory out of range");
            }
            return (int)result;
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Block device backed by nothing, for writing only. Data written to it will
     * simply be discarded without an error being raised.
     */
    public static class NullWriteDevice extends BlockDeviceImpl {
        public NullWriteDevice(int blocksz) {
            super(false, true, false, Long.MAX_VALUE, blocksz);
        }
        protected void internalRead(long num, byte[] block, int ofs) throws IOException {
            throw new IOException();
        }
        protected void internalWrite(long num, byte[] block, int ofs) throws IOException {
        }
        public void close(boolean err) {
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * To hook into the I/O of an existing block device.
     */
    public abstract static class HookBlockDevice implements BlockDevice {
        public HookBlockDevice(BlockDevice bdev) {
            this.bdev = bdev;
        }
        protected BlockDevice bdev;

        public int blockSize() {
            return this.bdev.blockSize();
        }
        public void read(long num, byte[] block, int ofs) throws IOException {
            if (onRead(num)) {
                this.bdev.read(num, block, ofs);
            }
            else {
                throw new AbortException();
            }
        }
        public boolean readOnly() {
            return this.bdev.readOnly();
        }
        public boolean serialWrite() {
            return this.bdev.serialWrite();
        }
        public long size() {
            return this.bdev.size();
        }
        public void write(long num, byte[] block, int ofs) throws IOException {
            if (onWrite(num)) {
                this.bdev.write(num, block, ofs);
            }
            else {
                throw new AbortException();
            }
        }
        public boolean writeOnly() {
            return this.bdev.writeOnly();
        }
        public void close(boolean err) throws IOException {
            this.bdev.close(err);
        }

        protected abstract boolean onRead (long num);
        protected abstract boolean onWrite(long num);
    }
}
