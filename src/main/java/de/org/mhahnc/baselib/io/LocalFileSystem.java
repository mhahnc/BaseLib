package de.org.mhahnc.baselib.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class LocalFileSystem implements FileSystem {
    static boolean _caseSensitive = 0 == new File("a").compareTo(new File("A"));

    ///////////////////////////////////////////////////////////////////////////

    boolean absolute;

    public LocalFileSystem(boolean absolute) {
        this.absolute = absolute;
    }

    class FNode extends FileNode {
        public FileSystem fileSystem() {
            return LocalFileSystem.this;
        }
        public char separatorChar() {
            return File.separatorChar;
        }
        public FNode initialize(File fl, FileNode parent, boolean root) {
            this.parent     = parent;
            this.name       = root ? fl.getPath() : fl.getName();
            this.size       = fl.length();
            this.timestamp  = fl.lastModified();
            this.attributes = (!fl.canWrite   () ? ATTR_READONLY  : 0) |
                              ( fl.isHidden   () ? ATTR_HIDDEN    : 0) |
                              ( fl.isDirectory() ? ATTR_DIRECTORY : 0) |
                              ( fl.canExecute () ? ATTR_EXECUTE   : 0);
            this.attributes |= root ? ATTR_ROOT : 0;
            return this;
        }
        public String link() {
            return this.link;
        }
        String link;
    }

    ///////////////////////////////////////////////////////////////////////////

    class NodeIterator implements Iterator<FileNode> {
        FileNode    next;
        FileNode    parent;
        String      link;
        File[]      files;
        Filter      filter;
        int         index = 0;
        boolean     roots;

        public NodeIterator(FileNode parent, File[] files, Filter filter, boolean roots) {
            boolean nn = null == parent ? false : (null == parent.name());
            this.parent = nn ? null : parent;
            this.link = nn ? parent.link() : null;
            this.files = files;
            this.roots = roots;
            this.filter = filter;
            lookup();
        }

        void lookup() {
            FNode fnode = new FNode();

            while (this.index < this.files.length) {
                fnode.initialize(
                        this.files[this.index++],
                        this.parent,
                        this.roots);
                fnode.link = this.link;

                if (null == this.filter || this.filter.matches(fnode)) {
                    this.next = fnode;
                    return;
                }
            }
            this.next = null;
        }

        public boolean hasNext() {
            return null != this.next;
        }

        public FileNode next() {
            FileNode result = this.next;

            if (null == result) {
                throw new NoSuchElementException();
            }

            lookup();

            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    public Iterator<FileNode> list(FileNode directory, Filter filter) throws IOException {
        String path = directory.path(true);
        File dir = new File(path);
        File[] files = dir.listFiles();
        files = null == files ? new File[0] : files;
        return new NodeIterator(
                directory,
                files,
                filter,
                false);
    }

    public void remove(FileNode obj) throws IOException {
        if (obj instanceof FNode fnode && new File(fnode.path(true)).delete()) {
            return;
        }
        throw new IOException();
    }

    public String scheme() {
        return "file";
    }

    public static boolean __test_FAKE_READ_ERROR;

    public InputStream openRead(FileNode file) throws IOException {
        final InputStream result = new FileInputStream(file.path(true));
        if (__test_FAKE_READ_ERROR) {
            return new InputStream() {
                public int available() throws IOException {
                    return result.available();
                }
                public void close() throws IOException {
                    result.close();
                }
                public synchronized void mark(int readlimit) {
                    result.mark(readlimit);
                }
                public boolean markSupported() {
                    return result.markSupported();
                }
                public synchronized void reset() throws IOException {
                    result.reset();
                }
                public long skip(long n) throws IOException {
                    return result.skip(n);
                }
                public int read(byte[] b, int ofs, int len) throws IOException {
                    throw new IOException("FAKE_ERROR_0");
                }
                public int read(byte[] b) throws IOException {
                    throw new IOException("FAKE_ERROR_1");
                }
                public int read() throws IOException {
                    throw new IOException("FAKE_ERROR_2");
                }
            };
        }
        return result;
    }

    public FileNode nodeFromString(String expr) throws IOException {
        FNode result;
        File rfl, fl = new File(expr);
        boolean root = true;
        String link = null;
        do {
            if (this.absolute) {
                rfl = fl = fl.getCanonicalFile();
                break;
            }
            if (fl.isAbsolute()) {
                rfl = fl;
                break;
            }
            String path = fl.getPath();
            if (0 < path.length() && File.separatorChar == path.charAt(0)) {
                rfl = fl.getCanonicalFile();
                break;
            }
            String[] rrp = IOUtils.resolveRelativePath(fl, IOUtils.currentPath());
            if (null == rrp[1]) {
                rfl = new File(rrp[0]);
                result = new FNode().initialize(rfl, null, IOUtils.isRoot(rfl));
                result.name = null;
                result.link = rrp[0];
                return result;
            }
            link = rrp[0];
            fl   = new File(rrp[1]);
            rfl  = new File(rrp[0], rrp[1]);
            root = false;
        }
        while(false);

        result = new FNode().initialize(rfl, null, IOUtils.isRoot(rfl));
        FNode last = result;

        File lastfl = null;
        for(;;) {
            fl  = fl .getParentFile();
            rfl = rfl.getParentFile();
            if (null == fl) {
                break;
            }
            last.parent = new FNode().initialize(rfl, null, IOUtils.isRoot(rfl));
            last = (FNode)last.parent;
            lastfl = fl;
        }
        if (root) {
            last.attributes |= FileNode.ATTR_ROOT;
            last.name = null == lastfl ? expr : lastfl.getAbsolutePath();
        }
        if (null != link) {
            last.link = link;
        }

        return result;
    }

    public Iterator<FileNode> roots() throws IOException {
        File[] roots = File.listRoots();
        return new NodeIterator(
                null,
                null == roots ? new File[0] : roots,
                null,
                true);
    }

    @Override
    public boolean equals(Object obj) {
        return null != obj && obj instanceof LocalFileSystem;
    }

    @Override
    public int hashCode() {
        return scheme().hashCode();
    }

    public boolean areNodesEqual(FileNode node1, FileNode node2, boolean recursive, boolean checkfs) {
        if (checkfs) {
            if (!node1.fileSystem().equals(node2.fileSystem())) {
                return false;
            }
        }
        if (_caseSensitive) {
            if (!node1.name.equals(node2.name)) {
                return false;
            }
        }
        else {
            if (!node1.name.equalsIgnoreCase(node2.name)) {
                return false;
            }
        }
        if (recursive) {
            if (null == node1.parent ^ null == node2.parent) {
                return false;
            }
            else if (null != node1.parent) {
                return areNodesEqual(node1.parent, node2.parent, recursive, false);
            }
        }
        return true;
    }

    public char separatorChar() {
        return File.separatorChar;
    }

    public boolean exists(FileNode fn) {
        return new File(fn.path(true)).exists();
    }
}
