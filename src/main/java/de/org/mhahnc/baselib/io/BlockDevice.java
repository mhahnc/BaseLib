package de.org.mhahnc.baselib.io;

import java.io.IOException;

/**
 * Block device definition.
 */
public interface BlockDevice {
    /** Thrown if an I/O got aborted, usually on the user level. */
    public static class AbortException extends IOException {
        private static final long serialVersionUID = 233514394958407198L;
    }

    /** Size of a single block in bytes. */
    int blockSize();

    /**
     * Number of blocks this device can handle.
     */
    long size();

    /** @return True if the device is read-only. */
    boolean readOnly();

    /** @return True if the device is write-only. */
    boolean writeOnly();

    /** @return True if the device can only be accessed in a serial fashion,
     * block after block, starting from block 0. */
    boolean serialWrite();

    /**
     * Writes a block to the device.
     * @param num The block number.
     * @param block The buffer holding the block data.
     * @param ofs Where the block data starts in the buffer.
     * @throws IOException If any error occurred.
     */
    void write(long num, byte[] block, int ofs) throws IOException;

    /**
     * Reads a block from the device.
     * @param num The number of the block to read.
     * @param block Buffer where to write the block data.
     * @param ofs Where to start writing the buffer.
     * @throws IOException If any error occurred.
     */
    void read (long num, byte[] block, int ofs) throws IOException;

    /**
     * Closes the block device.
     * @param err True if the stream failed and only cleanup should happen.
     * @throws IOException If any error occurred.
     */
    void close(boolean err) throws IOException;


    /**
     * To change a block's data and/or its location during processing.
     */
    interface Filter {
        /**
         * Maps a block number to a new location.
         * @param num The number of the block.
         * @return The new block number, might be the same as the hold one.
         */
        long map(long num);

        /**
         * Called if a block should be handled by the filter.
         * @param num The number of the block.
         * @param block Buffer with the block data.
         * @param ofs Where the block data starts in the buffer.
         * @throws IOException If any error occurred.
         */
        void transform(long num, byte[] block, int ofs) throws IOException;

        /** Filter factory definition. */
        interface Factory {
            /**
             * To initialize the factory.
             * @param blockSize Size of the blocks for the filters to expect.
             * @throws IOException If any error occurred.
             */
            void initialize(int blockSize) throws IOException;

            /** @return Create a filter for reading. */
            Filter createRead();

            /** @return Create a filter for writing. */
            Filter createWrite();
        }
    }
}
