package de.org.mhahnc.baselib.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class RandomAccessFileStream {
    public static OutputStream newOut(final RandomAccessFile raf) {
        return new OutputStream() {
            public void write (byte[] b)               throws IOException { raf.write(b); }
            public void write (byte[] b, int o, int l) throws IOException { raf.write(b, o, l); }
            public void flush ()                       throws IOException { }
            public void close ()                       throws IOException { raf.close(); }
            public void write(int b)                   throws IOException { raf.write(b); }
        };
    }
}
