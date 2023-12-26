package de.org.mhahnc.baselib.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import de.org.mhahnc.baselib.util.CompactMap;
import de.org.mhahnc.baselib.util.Taggable;

/**
 * In an abstracted file system a node represents either a file, a directory or
 * a root node.
 */
public abstract class FileNode implements Taggable {
    protected FileNode parent;
    protected String   name;
    protected long     size = -1L;
    protected long     timestamp;
    protected int      attributes;

    protected CompactMap<String, Object> tags = new CompactMap<>();

    public final static int ATTR_NONE      = 0;
    public final static int ATTR_READONLY  = 1;
    public final static int ATTR_HIDDEN    = 2;
    public final static int ATTR_EXECUTE   = 4;
    public final static int ATTR_ROOT      = 8;
    public final static int ATTR_DIRECTORY = 16;

    public abstract FileSystem fileSystem();

    /** @return Name of the file or directory. Root nodes might have names as
     * well, but they are an example where the name is empty. If a relative path
     * with no elements of its own go resolved the name will be null. */
    public String name() {
        return this.name;
    }

    /** @return Actual path of the node, to be used when assembling the
     * complete path expression. Can be null if no such thing or even concept,
     * in that case the name must be used. */
    protected String link() {
        return null;
    }

    /** @return The parent node. Always a directory or a root node. Root nodes
     * return null. */
    public FileNode parent() {
        return this.parent;
    }

    /** @return Size of the file. Directories and root notes return 0. */
    public long size() {
        return this.size;
    }

    /** @return Time-stamp of the node (of last write access). */
    public long timestamp() {
        return this.timestamp;
    }

    /** @return Attributes of the node, masked out of the ATTR_ constants. */
    public int attributes() {
        return this.attributes;
    }

    /**
     * To check if one (or more) attribute(s) match the node.
     * @param attr Attribute mask.
     * @return True if all of the attributes do match.
     */
    public boolean hasAttributes(int attr) {
        return attr == (attr & this.attributes);
    }

    /**
     * Resolved path of the node. Can be used with its original file system.
     * @param linked True if the linked bottom node should be resolved.
     * @return Absolute path.
     */
    public String path(boolean linked) {
        StringBuilder result = new StringBuilder();
        if (null != this.name) {
            result.append(this.name);
        }
        FileNode fn = this;

        final char   sepa    = fileSystem().separatorChar();
        final String sepastr = sepa + "";

        if (null != fn.parent) {
            fn = fn.parent;
            for (;;) {
                if (null == fn.parent) {
                    if (!fn.hasAttributes(ATTR_ROOT) || !fn.name().endsWith(sepastr)) {
                        result.insert(0, sepa);
                    }

                    result.insert(0, fn.name());
                    break;
                }

                result.insert(0, fileSystem().separatorChar());
                result.insert(0, fn.name());

                fn = fn.parent;
            }
        }

        if (linked) {
            String lnk = fn.link();
            if (null != lnk) {
                if (!lnk.endsWith(sepastr)) {
                    result.insert(0, sepa);
                }
                result.insert(0, lnk);
            }
        }

        return result.toString();
    }

    /** @see de.org.mhahnc.baselib.util.Taggable#getTag(java.lang.String) */
    public Object getTag(String name) {
        return this.tags.get(name);
    }

    /** @see de.org.mhahnc.baselib.util.Taggable#setTag(java.lang.String, java.lang.Object) */
    public void setTag(String name, Object tag) {
        this.tags.put(name, tag);
    }

    /** @see java.lang.Object#equals(java.lang.Object) */
    public boolean equals(Object obj) {
        if (obj instanceof FileNode fnode) {
            return fileSystem().areNodesEqual(this, fnode, true, true);
        }
        return false;
    }

    /** @see java.lang.Object#hashCode() */
    public int hashCode() {
        return this.name.hashCode();
    }

    /**
     * Determines the root node of this node.
     * @return The root node or null if the lowest node is not a root node.
     */
    public FileNode root() {
        FileNode result = this;
        for (;;) {
            if (null == result.parent) {
                return result.hasAttributes(ATTR_ROOT) ? result : null;
            }
            result = result.parent;
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Virtual file node, to be used as place holders, usually for directories
     * in composer-like implementations. They are backed by a virtual file
     * systems, which should not be accessed since most of its methods are not
     * available and will error out.
     */
    public static class Virtual extends FileNode {
        public Virtual(FileNode parent, String name,
                       long size, long timestamp, int attributes) {
            this.parent     = parent;
            this.name       = name;
            this.size       = size;
            this.timestamp  = timestamp;
            this.attributes = attributes;
        }

        public FileSystem fileSystem() {
            return new FileSystem() {
                void noavail(String method) throws IOException {
                    throw new IOException(String.format(
                            "method %s() not available on a virtual file node", method));
                }
                public boolean areNodesEqual(FileNode node1, FileNode node2,
                                             boolean recursive, boolean checkfs) {
                    return false;
                }
                public Iterator<FileNode> list(FileNode directory, Filter filter) throws IOException {
                    noavail("list");
                    return null;
                }
                public void remove(FileNode obj) throws IOException {
                    noavail("delete");
                }
                public FileNode nodeFromString(String expr) throws IOException {
                    noavail("nodeFromString");
                    return null;
                }
                public InputStream openRead(FileNode file) throws IOException {
                    noavail("openRead");
                    return null;
                }
                public Iterator<FileNode> roots() throws IOException {
                    noavail("roots");
                    return null;
                }
                public String scheme() {
                    return "virtual";
                }
                public char separatorChar() {
                    return '/';
                }
                public boolean exists(FileNode obj) throws IOException {
                    noavail("exists");
                    return false;
                }
            };
        }
    }
}
