package de.org.mhahnc.baselib.io;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import de.org.mhahnc.baselib.util.Combo;
import de.org.mhahnc.baselib.util.CompactMap;
import de.org.mhahnc.baselib.util.MiscUtils;

/**
 * To register files and directories, potentially from different file systems,
 * in one tree-structured collection: directories point to files or other
 * sub-directories. Notice that a node's physical path can look completely
 * different, only its name is taken over.
 */
public abstract class FileRegistrar {
    /** Directory definition */
    public interface Directory {
        /** @return The node(s) the directory is backed with. Due to merging it
         * is possible that there are multiple directory nodes. Which one is the
         * primary is up to the implementation, if it actually supports merging
         * at all. Very often the primary is the first node on the list. */
        FileNode[] nodes();
        /** @return The parent directory. */
        Directory parent();
        /** @return The (sub)directories. */
        Iterator<Directory> dirs();
        /** @return The files. */
        Iterator<FileNode> files();
    }

    final static String DIR_TAG_NAME = "dir.filereg";

    ///////////////////////////////////////////////////////////////////////////

    /**
     * When adding file nodes to the registrar collisions of nodes with equal
     * names might occur. To resolve such a conflict this callback is used.
     */
    @FunctionalInterface
    public interface Callback {
        /** Merge decision. */
        public enum Merge {
            /** Abort the registration process. */
            ABORT,
            /** Replace the old node with the new one. For directories this
             * means that the node becomes the primary one. */
            REPLACE,
            /** Leave the old node, ignore the new one. For directories this
             * means that the node is still registered, but put at the end of
             * the list. */
            IGNORE
        }

        /**
         * Invoked on node collision.
         * @param nd0 The old node(s).
         * @param nd1 The new node.
         * @return The merge decision.
         */
        Merge onMerge(FileNode[] nd0, FileNode nd1);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Adds new files to the registrar. Merging might be applied.
     * @param nodes The nodes to add. They all must share the same parent. The
     * nodes can either be of file or directory type, in latter case this will
     * just cause empty directories to be created.
     * @param bottom The lowest directory node to include from the path. Might
     * be null, in which case the whole path is kept, excluding the root node.
     * Can also be the node itself, in such a case no path is kept at all. If
     * the node is a root node it will be excluded.
     * @param parent In which directory to add/merge the files. If null the root
     * node of the registrar will be the target.
     * @param cb The merging decider.
     * @return True if successful or false if the decider aborted the operation.
     * @throws IOException If any error occurred.
     */
    public abstract boolean add(
            List<FileNode> nodes, FileNode bottom,
            Directory parent, Callback cb) throws IOException;

    /**
     * @return The root directory of the registrar.
     */
    public abstract Directory root();

    /**
     * Returns the directory a file node is registered with.
     * @param nd The file node.
     * @return The directory the node is registered with. Might not be the same
     * registrar instance. Null if the node is not associated with anything.
     */
    public abstract Directory getNodeDirectory(FileNode nd);

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Dumps the registrar's content.
     * @param ps Where to print the information.
     */
    public static void dump(Directory dir, int lvl, PrintStream ps) {
        final String spaces = MiscUtils.fillString(lvl << 1, ' ');

        StringBuilder snodes = new StringBuilder();
        FileNode[] nodes = dir.nodes();
        if (null != nodes) {
            for (FileNode node : nodes) {
                if (0 < snodes.length()) {
                    snodes.append('|');
                }
                snodes.append(node.name);
            }
        }
        ps.printf("%s[%s]\n", spaces, snodes.toString());

        final Iterator<FileNode> ifn = dir.files();
        while (ifn.hasNext()) {
            final FileNode fn = ifn.next();
            ps.printf("%s%s (%d)\n", spaces, fn.name, fn.size);
        }

        final Iterator<Directory> idir = dir.dirs();
        while (idir.hasNext()) {
            dump(idir.next(), lvl + 1, ps);
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * File registrar instance backed by memory, consider the number of files
     * to register, to avoid out-of-memory errors. Directories and files are
     * sorted in a case-insensitive fashion.
     */
    public static class InMemory extends FileRegistrar {

        final Dir                root;
        final Comparator<String> cmp;

        FileNode[] nodePath = new FileNode[32];

        /**
         * @param cmp Comparator for object names.
         */
        public InMemory(Comparator<String> cmp) {
            this.cmp = cmp;
            this.root = new Dir(new FileNode.Virtual(
                null, "", 0, -1L,
                FileNode.ATTR_DIRECTORY |
                FileNode.ATTR_ROOT), null);
        }

        /**
         * Comparator which offers case-sensitive and insensitive operations.
         */
        public static class DefCmp implements Comparator<String> {
            public DefCmp(boolean caseMerge) {
                this.caseMerge = caseMerge;
            }
            public int compare(String s1, String s2) {
                return this.caseMerge ?
                    s1.compareTo          (s2) :
                    s1.compareToIgnoreCase(s2);
            }
            final boolean caseMerge;
        }

        class Dir implements Directory {

            Map<String, Directory> dirs  = new TreeMap<>(InMemory.this.cmp);
            Map<String, FileNode > files = new TreeMap<>(InMemory.this.cmp);

            Directory  parent;
            FileNode[] nodes;

            public Dir(FileNode node, Directory parent) {
                node.setTag(DIR_TAG_NAME, this);
                this.nodes  = new FileNode[] { node };
                this.parent = parent;
            }
            public Iterator<Directory> dirs() {
                return this.dirs.values().iterator();
            }
            public Iterator<FileNode> files() {
                return this.files.values().iterator();
            }
            public Directory parent() {
                return this.parent;
            }
            public FileNode[] nodes() {
                return this.nodes;
            }
            @Override
            public String toString() {
                return String.format("'%s', %d nodes, %d dirs, %d files, %s parent",
                        this.nodes[0].name,
                        this.nodes.length,
                        this.dirs.size(),
                        this.files.size(),
                        null == this.parent ? "no" : "has");
            }
        }

        static class VFileNode extends FileNode {
            public VFileNode(String name, FileNode realNode) {
                this.name = name == null?
                            name : realNode.name;

                if (null == realNode) {
                    this.timestamp  = System.currentTimeMillis();
                    this.attributes = ATTR_DIRECTORY;
                }
                else {
                    this.parent     = realNode.parent;
                    this.size       = realNode.size;
                    this.timestamp  = realNode.timestamp;
                    this.attributes = realNode.attributes;
                    this.tags       = new CompactMap<>(realNode.tags);
                }
            }

            public FileSystem fileSystem() {
                return null;
            }
        }

        public Directory root() {
            return this.root;
        }

        // TODO: a bit of a design flaw, the addition should always be the same,
        //       only the storage of the nodes would be abstracted, revisit...

        public boolean add(List<FileNode> nodes, FileNode bottom,
                           Directory parent, Callback cb) throws IOException {
            if (0 == nodes.size()) {
                return true;
            }
            final FileNode node = nodes.get(0);
            final boolean self = bottom == node;
            FileNode nd = self ? node : node.parent;
            int pathDepth = 0; // excluding the file node
            for (;;) {
                pathDepth++;
                if (bottom == nd) {
                    if (bottom == null                           ||
                        bottom.hasAttributes(FileNode.ATTR_ROOT) ||
                        self) {
                        pathDepth--;
                    }
                    break;
                }
                else if (null == nd) {
                    throw new IOException("unexpected null node encountered");
                }
                else if (0 != (nd.attributes() & FileNode.ATTR_ROOT))
                {   // root nodes must not be included
                    pathDepth--;
                    break;
                }
                nd = nd.parent;
            }
            if (pathDepth > this.nodePath.length) {
                this.nodePath = new FileNode[pathDepth << 1];
            }
            FileNode[] nodePath = this.nodePath;
            nd = node.parent;
            for (int i = pathDepth - 1; i >= 0; i--) {
                nodePath[i] = nd;
                nd = nd.parent;
            }
            Dir dir = null == parent ? this.root : (Dir)parent;
            for (int i = 0; i < pathDepth; i++) {
                nd = nodePath[i];
                Dir dir2 = (Dir)(dir.dirs.get(nd.name()));
                if (null == dir2) {
                    dir2 = new Dir(nd, dir);
                    dir.dirs.put(nd.name(), dir2);
                }
                else {
                    for (FileNode dn : dir2.nodes()) {
                        if (dn == nd) {
                            nd = null;
                            break;
                        }
                    }
                    if (null != nd) {
                        int l = dir2.nodes.length;
                        int pos, cpy;
                        switch(cb.onMerge(dir2.nodes(), nd)) {
                            case ABORT  : return false;
                            case REPLACE: pos = 0; cpy = 1; break;
                            case IGNORE : cpy = 0; pos = l; break;
                            default: throw new IOException();
                        }
                        // TODO: rather than just blindly adding the directory
                        // node we should(?) actually check if an identical one
                        // is already in the list, and by that avoid duplicates,
                        // which are different as an instance, but in reality
                        // just really point to one and the same thing...
                        FileNode[] nnodes = new FileNode[l + 1];
                        nnodes[pos] = nd;
                        System.arraycopy(dir2.nodes, 0, nnodes, cpy, l);
                        dir2.nodes = nnodes;
                    }
                }
                dir = dir2;
            }
            FileNode nd2_2 = null;
            for (FileNode nd2 : nodes) {
                if (null != nd2_2 && nd2.parent != nd2_2.parent) {
                    throw new IOException("node link conflict");
                }
                nd2_2 = nd2;
                if (nd2.hasAttributes(FileNode.ATTR_DIRECTORY)) {
                    Directory dir2 = dir.dirs.get(nd2.name());
                    boolean replace = true;
                    if (null != dir2) {
                        final FileNode[] dns = dir2.nodes();
                        for (FileNode dn : dns) {
                            if (dn == nd2) {
                                replace = false;
                                break;
                            }
                        }
                        if (replace) {
                            switch(cb.onMerge(dns, nd2)) {
                                case ABORT  : return false;
                                case REPLACE: break;
                                case IGNORE : replace = false; break;
                            }
                        }
                    }
                    if (replace) {
                        Dir emptyDir = new Dir(nd2, dir);
                        dir.dirs.put(nd2.name(), emptyDir);
                    }
                }
                else {
                    FileNode nd3 = dir.files.get(nd2.name());
                    boolean replace = true;
                    if (null != nd3) {
                        switch(cb.onMerge(new FileNode[] { nd3 }, nd2)) {
                            case ABORT  : return false;
                            case REPLACE: break;
                            case IGNORE : replace = false; break;
                        }
                    }
                    if (replace) {
                        nd2.setTag(DIR_TAG_NAME, dir);
                        dir.files.put(nd2.name(), nd2);
                    }
                }
            }
            return true;
        }

        public Directory getNodeDirectory(FileNode nd) {
            Object obj = nd.getTag(DIR_TAG_NAME);
            if (!(obj instanceof Dir)) {
                return null;
            }
            return (Dir)obj;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            dirToStr(result, this.root, 0);
            return result.toString();
        }
        void dirToStr(StringBuilder sb, Dir dir, int lvl) {
            sb.append(MiscUtils.fillString(lvl << 1, '-'));
            sb.append(dir.toString());
            sb.append('\n');
            Iterator<Directory> id = dir.dirs();
            while (id.hasNext()) {
                dirToStr(sb, (Dir)id.next(), lvl + 1);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Callback used for bulk registration, since it can take a long time if
     * many files on file system have to be found.
     */
    public interface BulkCallbacks extends Callback, FileSystem.Filter {
        /**
         * Called on every new directory node found.
         * @param current The currently registered (directory) node.
         * @return False if registration should be aborted.
         */
        boolean onProgress(FileNode current);
    }

    /** Bulk registration failed: an internal error occurred. */
    public final static int BULKERR_INTERNAL = -1;
    /** Bulk registration failed: aborted in a progress callback. */
    public final static int BULKERR_ABORTED = -2;
    /** Bulk registration failed: file name collision detected. */
    public final static int BULKERR_COLLISION = -3;

    /**
     * Bulk registration of a directory's files (and sub-directories).
     * @param freg The file registrar to add to.
     * @param dir The directory to add.
     * @param bottom Determines how much of the path to register. Can be null
     * for the whole path minus the root node, or the node itself for no path.
     * @param parent Where to add in the file registrar.
     * @param bcb Callback, to report progress.
     * @param inclSubDirs True to go register recursively.
     * @param addEmptyDirs True if empty directories should be registered also.
     * @return Number of files added, or a BULK_xxx code.
     * @throws IOException If any error occurred.
     */
    public static int bulk(FileRegistrar freg,
                           FileNode      dir,
                           FileNode      bottom,
                           Directory     parent,
                           BulkCallbacks  bcb,
                           boolean       inclSubDirs,
                           boolean       addEmptyDirs) throws IOException {
        if (!dir.hasAttributes(FileNode.ATTR_DIRECTORY)) {
            return BULKERR_INTERNAL;
        }
        if (!bcb.onProgress(dir)) {
            return BULKERR_ABORTED;
        }

        int result = 0;

        final FileSystem fsys = dir.fileSystem();

        final Stack<Combo.Two<Iterator<FileNode>, List<FileNode>>> itrs = new Stack<>();

        Iterator<FileNode> itr   = fsys.list(dir, bcb);
        List    <FileNode> items = new ArrayList<>();

        itrs.push(new Combo.Two<>(itr, items));

        for(;;) {
            if (itr.hasNext()) {
                FileNode fn = itr.next();

                if (bottom        == dir &&
                    bottom.name() == null) {
                    bottom = fn.parent();  // the '.' case
                }

                if (fn.hasAttributes(FileNode.ATTR_DIRECTORY)) {
                    if (!inclSubDirs) {
                        continue;
                    }
                    if (!bcb.onProgress(fn)) {
                        return BULKERR_ABORTED;
                    }
                    Iterator<FileNode> itr2 = fsys.list(fn, bcb);
                    if (itr2.hasNext()) {
                        itrs.push(new Combo.Two<>(
                                itr   = itr2,
                                items = new ArrayList<>()));
                    }
                    else if (addEmptyDirs) {
                        items.add(fn);
                    }
                }
                else {
                    items.add(fn);
                    result++;
                }
            }
            else {
                if (0 == items.size()   &&
                    addEmptyDirs        &&
                     dir.name() != null &&
                    !dir.hasAttributes(FileNode.ATTR_ROOT)) {
                    items.add(dir); // special case when bottom dir is all empty
                }
                if (0 < items.size()) {
                    if (!freg.add(items, bottom, parent, bcb)) {
                        return BULKERR_COLLISION;
                    }
                }
                itrs.pop();
                if (itrs.empty()) {
                    break;
                }
                itr   = itrs.peek().t;
                items = itrs.peek().u;
            }
        }
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns the path of the node in presentation of its file system.
     * @param nd The node.
     * @return The path (relative or absolute).
     */
    public static String nodePath(FileNode nd) {
        Object tag = nd.getTag(DIR_TAG_NAME);
        if (!(tag instanceof Directory)) {
            return null;
        }
        Directory dir = (Directory)tag;
        boolean isdir = nd.hasAttributes(FileNode.ATTR_DIRECTORY);
        StringBuilder result = new StringBuilder(isdir ? "" : nd.name());
        char sepa = nd.fileSystem().separatorChar();
        while (null != dir.parent()) {
            result.insert(0, sepa);
            result.insert(0, dir.nodes()[0].name());
            dir = dir.parent();
        }
        return result.toString();
    }

    ///////////////////////////////////////////////////////////////////////////

    @FunctionalInterface
    public interface Walker {
        boolean onNodes(FileNode[] fn);

        public static class Counting implements Walker {
            public int  files;
            public int  directories;
            public long bytesTotal;

            public boolean onNodes(FileNode[] fn) {
                if (fn[0].hasAttributes(FileNode.ATTR_DIRECTORY)) {
                    this.directories++;
                }
                else {
                    this.files++;
                    this.bytesTotal += fn[0].size();
                }
                return true;
            }
        }
    }

    /**
     * Walks a directory.
     * @param dir The directory. Can be the root of the registrar.
     * @param walker The walker callback.
     * @param recursive True if sub-directories should be visited also.
     * @param endNodesOnly True for only files or empty directories be reported.
     * @return True if the walker callback never returned false.
     */
    public static boolean walk(Directory dir, Walker walker, boolean recursive, boolean endNodesOnly) {
        Iterator<Directory> id  = dir.dirs();
        Iterator<FileNode>  ifn = dir.files();
        boolean empty = !id.hasNext() && !ifn.hasNext();
        while (id.hasNext()) {
            Directory dir2 = id.next();
            if (recursive) {
                if (!walk(dir2, walker, true, endNodesOnly)) {
                    return false;
                }
            }
        }
        FileNode[] fn = new FileNode[1];
        while (ifn.hasNext()) {
            fn[0] = ifn.next();
            if (!walker.onNodes(fn)) {
                return false;
            }
        }
        if (((endNodesOnly && empty) || !endNodesOnly) &&
            !dir.nodes()[0].hasAttributes(FileNode.ATTR_ROOT)) {
            if (!walker.onNodes(dir.nodes())) {
                return false;
            }
        }
        return true;
    }
}
