package de.org.mhahnc.baselib.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * File system abstraction for read-only purposes.
 */
public interface FileSystem {
    /**
     * The file system scheme. Useful for example for URL handling.
     * @return The scheme.
     */
    String scheme();

    /**
     * Tries to form a node from an expression.
     * @param expr The expression to examine.
     * @return The node. This might not be a physical node.
     * @throws IOException If the expression wasn't understood.
     */
    FileNode nodeFromString(String expr) throws IOException;

    /**
     * Checks if two nodes are equal. Both have to belong to the same type of
     * file system as the very base criteria. It is up to the implementation to
     * decide what equality really means, e.g. regarding case sensitivity.
     * @param node1 The first node.
     * @param node2 The second node.
     * @param recursive True to check if the whole path should be matched.
     * @param checkfs True to check if the file systems are equal or not.
     * @return True if the nodes are equal, false if not.
     */
    boolean areNodesEqual(FileNode node1, FileNode node2, boolean recursive, boolean checkfs);

    /**
     * List all nodes of a directory.
     * @param directory The directory node.
     * @param filter The filter to apply.
     * @return List of nodes, both files and directories.
     * @throws IOException If any error occurred.
     */
    Iterator<FileNode> list(FileNode directory, Filter filter) throws IOException;

    /**
     * Lists all of the root nodes. Some file systems might have just one root
     * node, other ones (e.g. Windows) might have multiple ones.
     * @return List of root nodes.
     * @throws IOException If any error occurred.
     */
    Iterator<FileNode> roots() throws IOException;

    /**
     * The separation character used in path expressions.
     * @return Separation character.
     */
    char separatorChar();

    /**
     * Opens a file node to stream out its content.
     * @param file The file node to open.
     * @return The stream where to read from.
     * @throws IOException If any error occurred.
     */
    InputStream openRead (FileNode file) throws IOException;

    /**
     * Removes a file or directory.
     * @param obj The object to remove.
     * @throws IOException If any error occurred.
     */
    void remove(FileNode obj) throws IOException;

    /**
     * Checks if a node is physically existing already. Notice that this might
     * not always be such an easy operation on some file systems (requiring an
     * actual open attempt).
     * @param obj The object to check (can be a file or a directory).
     * @return True if the node exists, false if not.
     * @throws IOException If any error occurred.
     */
    boolean exists(FileNode obj) throws IOException;

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Filter definition to traverse a file systems' directories.
     */
    @FunctionalInterface
    public interface Filter {
        /**
         * Called on every node before it is surfaced.
         * @param file The node.
         * @return True if the node matches the filter.
         */
        boolean matches(FileNode file);

        public final static Filter MATCHALL = new Filter() {
            public boolean matches(FileNode file) {
                return true;
            }
        };
    }
}
