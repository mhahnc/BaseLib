package de.org.mhahnc.baselib.util;

import java.util.Arrays;

/**
 * Pointer solution to access certain areas in byte arrays without having to
 * pass the classical buf+ofs+len combination around.
 */
public class BytePtr {

    public byte[] buf;
    public int ofs;
    public int len;

    public BytePtr(byte[] buf, int ofs, int len) {
        this.buf = buf;
        this.ofs = ofs;
        this.len = len;
    }

    public BytePtr(byte[] buf) {
        this.buf = buf;
        this.len = buf.length;
    }

    public final static BytePtr NO_DATA = new BytePtr(new byte[0]);

    /**
     * Makes a copy of the content it is pointing to and sets the offset to
     * zero. The length stays the same. Useful to stay valid if the underlying
     * buffer is volatile or not read-only.
     * @return Self reference.
     */
    public BytePtr grab() {
        if (null != this.buf) {
            byte[] newBuf = new byte[this.len];
            System.arraycopy(this.buf, this.ofs, newBuf, 0, this.len);
            this.buf = newBuf;
            this.ofs = 0;
        }
        return this;
    }

    public byte at(int index) {
        return this.buf[this.ofs + index];
    }

    public int end() {
        return this.ofs + this.len;
    }

    /**
     * @return Full copy of the data the pointer references.
     */
    public byte[] extract() {
        byte[] result = new byte[this.len];
        System.arraycopy(this.buf, this.ofs, result, 0, this.len);
        return result;
    }

    /**
     * Writes the data the pointer references to an output buffer.
     * @param buf Output buffer, must be of sufficient size.
     * @param ofs Where to start writing in the output buffer.
     */
    public void write(byte[] buf, int ofs) {
        System.arraycopy(this.buf, this.ofs, buf, ofs, this.len);
    }

    /**
     * @return True if the data the pointer references is accessible. False if
     * not, meaning it would be an out-of-bounds issue.
     */
    public boolean isValid() {
        if (0 == this.len && this.ofs == this.buf.length) {
            return true;     // special case, seen in the wild
        }
        else if (this.ofs < 0) {
            return false;
        }
        else if (this.ofs >= this.buf.length) {
            return false;
        }
        int end = this.ofs + this.len;
        if (end < 0) {
            return false;
        }
        else if (end > this.buf.length) {
            return false;
        }
        return true;
    }

    /**
     * Fills the referenced area with zeros.
     */
    public void clear() {
        Arrays.fill(this.buf, this.ofs, this.ofs + this.len, (byte)0);
    }

    @Override
    public String toString() {
        return String.format("ofs=%d,end=%d,len=%d,buf.len=%d",
                this.ofs, end(), this.len, this.buf.length);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BytePtr bp) {
            return this.len == bp.len &&
                BinUtils.arraysEquals(this.buf, this.ofs, bp.buf, bp.ofs, bp.len);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.len ^ this.ofs;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Pointer which validates its range in the constructor.
     */
    public static class Checked extends BytePtr {
        public Checked(byte[] buf, int ofs, int len) throws IllegalArgumentException {
            super(buf, ofs, len);
            if (!isValid()) {
                throw new IllegalArgumentException(String.format(
                        "byteptr (%d, %d) is invalid (buf.len=%d)",
                        ofs, len, buf.length));
            }
        }
    }
}
