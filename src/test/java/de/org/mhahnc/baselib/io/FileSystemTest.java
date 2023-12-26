package de.org.mhahnc.baselib.io;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import org.junit.Test;

public class FileSystemTest {
    static class DummyFileNode extends FileNode {
        public FileSystem fileSystem() {
            return new FileSystem() {
                public boolean areNodesEqual(FileNode node1, FileNode node2,
                        boolean recursive, boolean checkfs) {
                    throw new IllegalArgumentException();
                }
                public Iterator<FileNode> list(FileNode directory, Filter filter)
                        throws IOException {
                    throw new IllegalArgumentException();
                }
                public void remove(FileNode obj) throws IOException {
                    throw new IllegalArgumentException();
                }
                public FileNode nodeFromString(String expr) throws IOException {
                    throw new IllegalArgumentException();
                }
                public InputStream openRead(FileNode file) throws IOException {
                    throw new IllegalArgumentException();
                }
                public Iterator<FileNode> roots() throws IOException {
                    throw new IllegalArgumentException();
                }
                public String scheme() {
                    throw new IllegalArgumentException();
                }
                public char separatorChar() {
                    return '|';
                }
                public boolean exists(FileNode obj) throws IOException {
                    throw new IllegalArgumentException();
                }
            };
        }
    }

    @Test
    public void testFileNode() {
        FileNode fnode = new DummyFileNode();

        fnode.parent = new DummyFileNode();
        fnode.parent.name = "lower";
        fnode.parent.parent = new DummyFileNode();
        fnode.parent.parent.name = "lowest";

        fnode.attributes = 0x0f;
        fnode.name = "the_node";
        fnode.size = 100001;
        fnode.timestamp = System.currentTimeMillis();

        String path = fnode.path(false);
        assertEquals("lowest|lower|the_node", path);

        assertTrue (fnode.hasAttributes(FileNode.ATTR_READONLY));
        assertTrue (fnode.hasAttributes(FileNode.ATTR_HIDDEN));
        assertTrue (fnode.hasAttributes(FileNode.ATTR_EXECUTE));
        assertTrue (fnode.hasAttributes(FileNode.ATTR_ROOT));
        assertFalse(fnode.hasAttributes(FileNode.ATTR_DIRECTORY));

        assertTrue(100001 == fnode.size());
        assertTrue((FileNode.ATTR_READONLY |
                    FileNode.ATTR_HIDDEN   |
                    FileNode.ATTR_EXECUTE  |
                    FileNode.ATTR_ROOT) == fnode.attributes());

        FileNode root = fnode.root();
        assertNull(fnode.root());

        fnode.parent.parent.attributes |= FileNode.ATTR_ROOT;
        root = fnode.root();
        assertTrue(fnode.parent.parent == root);
        assertEquals(root.name(), "lowest");
    }
}
