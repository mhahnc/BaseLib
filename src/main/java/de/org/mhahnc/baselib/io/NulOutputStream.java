package de.org.mhahnc.baselib.io;

import java.io.IOException;
import java.io.OutputStream;

public class NulOutputStream extends OutputStream {
    public NulOutputStream() {
    }
    public void write(byte[] buf, int ofs, int len) throws IOException {
        super.write(buf, ofs, len);
    }
    public void write(int b) {
    }
}
