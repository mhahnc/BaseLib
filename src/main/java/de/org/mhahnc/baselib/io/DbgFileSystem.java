package de.org.mhahnc.baselib.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.zip.Adler32;

import de.org.mhahnc.baselib.util.BinUtils;

public class DbgFileSystem implements FileSystem {
    class FNode extends FileNode {
        public FileSystem fileSystem() {
            return DbgFileSystem.this;
        }
    }
    class DNode extends FNode {
        public Dir dir;
    }

    static class Dir {
        public DNode                 node;
        public Map<String, FileNode> subnodes = new HashMap<>();
    }

    ///////////////////////////////////////////////////////////////////////////

    final Map<String, Dir> roots = new HashMap<>();
    final boolean useRnd;

    ///////////////////////////////////////////////////////////////////////////

    public DbgFileSystem(boolean useRnd, Character sepaChar) {
        this.useRnd = useRnd;
        this.sepaChar = null == sepaChar ? '/' : sepaChar;
    }

    public boolean areNodesEqual(FileNode node1, FileNode node2,
                                 boolean recursive, boolean checkfs) {
        final int ATTRS = FileNode.ATTR_DIRECTORY | FileNode.ATTR_ROOT;

        return !(node1.hasAttributes(ATTRS) ^
                 node2.hasAttributes(ATTRS)) &&
               recursive ?
               node1.path(false).equals(node2.path(false)) :
               node1.name()     .equals(node2.name());
    }

    public Iterator<FileNode> list(FileNode directory, final Filter filter) throws IOException {
        if (directory instanceof final DNode dn) {

            return new Iterator<FileNode>() {
                Iterator<FileNode> i = dn.dir.subnodes.values().iterator();
                FileNode nextfn = lookahead();
                public boolean hasNext() {
                    return null != this.nextfn;
                }
                public FileNode next() {
                    FileNode result = this.nextfn;
                    if (null == result) {
                        throw new NoSuchElementException();
                    }
                    this.nextfn = lookahead();
                    return result;
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
                FileNode lookahead() {
                    while (this.i.hasNext()) {
                        FileNode result = this.i.next();
                        if (filter.matches(result)) {
                            return result;
                        }
                    }
                    return null;
                }
            };
        }
        throw new IOException("illegal file node");
    }

    public FileNode nodeFromString(String expr) throws IOException {
        final String sepaStr = this.sepaChar + "";

        String[] parts = expr.split(Pattern.quote(sepaStr));
        if (0 == parts.length) {
            return null;
        }

        Dir dir = this.roots.get(parts[0]);
        if (null == dir) {
            return null;
        }

        FileNode result = dir.node;

        for (int i = 1; i < parts.length; i++) {
            result = dir.subnodes.get(parts[i]);
            if (null == result) {
                return null;
            }
            if (result.hasAttributes(FileNode.ATTR_DIRECTORY)) {
                dir = ((DNode)result).dir;
                continue;
            }
            if (i == parts.length - 1) {
                break;
            }
            return null;
        }

        return result;
    }

    public final static int MAX_NORND_READ_SZ = 4096;

    public static InputStream createInputStream(
            String fname, final long flen, boolean useRnd) {
        if (useRnd) {
            Adler32 att = new Adler32();
            byte[] buf = new byte[8];
            BinUtils.writeInt64BE(flen, buf, 0);
            att.update(buf);
            att.update(fname.getBytes());
            final Random rnd = new Random(att.getValue());

            return new InputStream() {
                long left = flen;
                int r, n;
                public int read() throws IOException {
                    if (0 == this.left) {
                        return -1;
                    }
                    this.left--;
                    if (0 == this.n) {
                        this.r = rnd.nextInt();
                        this.n = 3;
                    }
                    else {
                        this.r >>>= 8;
                        this.n--;
                    }
                    return this.r & 0x0ff;
                }
            };
        }
        final byte[] fill = new byte[MAX_NORND_READ_SZ];
        Arrays.fill(fill, (byte)'a');
        return new InputStream() {
            long left = flen;
            public int read(byte[] buf, int ofs, int len) {
                if (0 == this.left) {
                    return -1;
                }
                int result = this.left > len ? len : (int)this.left;
                result = Math.min(fill.length, result);
                System.arraycopy(fill, 0, buf, 0, result);
                this.left -= result;
                return result;
            }
            final byte[] oneByte = new byte[1];
            public int read() throws IOException {
                final int read = read(this.oneByte);
                return -1 == read ? -1 : read & 0x0ff;
            }
        };
    }

    public InputStream openRead(final FileNode fn) throws IOException {
        if (!(fn instanceof FNode) ||
              fn instanceof DNode) {
            throw new IOException("illegal node type");
        }
        return createInputStream(fn.name(), fn.size(), this.useRnd);
    }

    public OutputStream openWrite(FileNode fn) throws IOException {
        throw new IOException();
    }

    public Iterator<FileNode> roots() throws IOException {
        return new Iterator<FileNode>() {
            Iterator<Entry<String, Dir>> dirs =
                DbgFileSystem.this.roots.entrySet().iterator();
            public boolean hasNext() {
                return this.dirs.hasNext();
            }
            public FileNode next() {
                return this.dirs.next().getValue().node;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public String scheme() {
        return "dbg";
    }

    public char separatorChar() {
        return this.sepaChar;
    }
    char sepaChar;

    ///////////////////////////////////////////////////////////////////////////

    class DbgFileNode extends FileNode {
        public FileSystem fileSystem() {
            return DbgFileSystem.this;
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    Dir makeDir(String name, DNode parent) {
        Dir result = new Dir();
        result.node = new DNode();
        result.node.name       = name;
        result.node.attributes = FileNode.ATTR_DIRECTORY;
        result.node.parent     = parent;
        result.node.dir        = result;
        return result;
    }

    /**
     * Create a new file, or just a directory path.
     * @param name The name of the file, null if a directory should be created.
     * @param path The path to create if not doesn't exist yet. Each element is
     * a directory entry, except of path[0] being the root name.
     * @param sz The size of the file, ignored if the name is null.
     * @param tstamp The time stamp of the file or directory.
     * @param attrs The attributes, the directory bit is added if name is null.
     * @param replace True if an existing file node should be replaced or false
     * if we should back off and return false. Ignored if the name is null.
     * @return The created node (or topmost directory), or null on conflict.
     * @throws IOException If any error occurred.
     */
    public FileNode createFile(String name, String[] path,
                               long sz, long tstamp, int attrs,
                               boolean replace) throws IOException {
        if (0 == path.length) {
            throw new IOException("empty path");
        }

        String nm = path[0];
        Dir dir = this.roots.get(nm);
        if (null == dir) {
            this.roots.put(nm, dir = makeDir(path[0], null));
            dir.node.attributes |= FileNode.ATTR_ROOT;
        }

        for (int i = 1; i < path.length; i++) {
            nm = path[i];
            FNode fn = (FNode)dir.subnodes.get(nm);
            if (null == fn) {
                Dir dir2 = makeDir(nm, dir.node);
                dir.subnodes.put(nm, dir2.node);
                dir = dir2;
            }
            else {
                if (fn instanceof DNode dnode) {
                    dir = dnode.dir;
                }
                else {
                    throw new IOException(String.format(
                            "path element '%s' is a file", nm));
                }
            }
        }

        if (null == name) {
            dir.node.timestamp   = tstamp;
            dir.node.attributes |= attrs;
            return dir.node;
        }
        if (!replace && null != dir.subnodes.get(name)) {
            return null;
        }

        FNode fn = new FNode();
        fn.name       = name;
        fn.attributes = attrs;
        fn.parent     = dir.node;
        fn.size       = sz;
        fn.timestamp  = tstamp;

        dir.subnodes.put(name, fn);

        return fn;
    }

    /**
     * Adds an (empty) root.
     * @param root Name of the root to add.
     * @return True if it has been added, false if such a root exists already.
     */
    public boolean addRoot(String root) {
        if (this.roots.containsKey(root)) {
            return false;
        }

        final Dir rdir = makeDir(root, null);
        rdir.node.attributes |= FileNode.ATTR_ROOT;

        this.roots.put(root, rdir);

        return true;
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String verifyNodeStrean(
            InputStream ins, FileNode nd, int bufSz) throws IOException {
        InputStream ins2 = null;
        try {
            ins2 = nd.fileSystem().openRead(nd);

            byte[] buf  = new byte[Math.max(bufSz, 1)];
            byte[] buf2 = new byte[buf.length];

            long c = nd.size();

            while (c > 0) {
                int toread = buf.length > c ? (int)c : buf.length;
                c -= toread;

                int read = IOUtils.readAll(ins, buf, 0, toread);
                if (read != toread) {
                    return String.format(
                            "read underrun on source stream, %d bytes missing",
                            toread - read);
                }
                int read2 = IOUtils.readAll(ins2, buf2, 0, toread);
                if (read2 != toread) {
                    return String.format(
                            "read underrun on node stream, %d bytes missing",
                            toread - read);
                }

                if (!BinUtils.arraysEquals(buf, 0, buf2, 0, toread)) {
                    return "data mismatch";
                }
            }
            if (-1 != ins.read()) {
                return "source stream oversize";
            }
            if (-1 != ins2.read()) {
                return "node stream too long!?";
            }
            return null;
        }
        finally {
                              try { ins .close(); } catch (IOException ignored) { }
            if (null != ins2) try { ins2.close(); } catch (IOException ignored) { }
        }
    }

    public void remove(FileNode obj) throws IOException {
        throw new IOException();
    }

    public boolean exists(FileNode obj) throws IOException {
        return false;
    }
}
