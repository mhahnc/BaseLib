package de.org.mhahnc.baselib.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import de.org.mhahnc.baselib.io.FileRegistrar.Directory;
import de.org.mhahnc.baselib.io.FileRegistrar.InMemory.DefCmp;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class FileRegistrarTest implements FileRegistrar.Callback {
    static List<FileNode> nodeToList(FileNode fn) {
        List<FileNode> result = new LinkedList<>();
        result.add(fn);
        return result;
    }

    @Test
    public void test0() throws IOException {
        FileRegistrar freg = new FileRegistrar.InMemory(new DefCmp(false));
        assertNotNull(freg.root());
        assertNotNull(freg.root().files());
        assertNotNull(freg.root().dirs());
        assertFalse(freg.root().files().hasNext());
        assertFalse(freg.root().dirs().hasNext());

        LocalFileSystem lfs = new LocalFileSystem(false);
        final char SC = lfs.separatorChar();

        // relative path
        String path = "a" + SC + "b" + SC + "c.txt";
        FileNode nd = lfs.nodeFromString(path);
        assertNull(freg.getNodeDirectory(nd));
        assertNotNull(nd.parent());
        assertNotNull(nd.parent().parent());
        assertNull(nd.parent().parent().parent());
        assertTrue(freg.add(nodeToList(nd), null, null, this));
        Iterator<FileRegistrar.Directory> dirs = freg.root().dirs();
        assertTrue(dirs.hasNext());
        FileRegistrar.Directory dir = dirs.next();
        assertFalse(dirs.hasNext());
        assertFalse(dir.files().hasNext());
        assertTrue(dir.parent() == freg.root());
        assertEquals("a", dir.nodes()[0].name());
        dirs = dir.dirs();
        assertTrue(dirs.hasNext());
        FileRegistrar.Directory dir2 = dirs.next();
        assertFalse(dirs.hasNext());
        assertTrue(dir2.parent() == dir);
        assertEquals("b", dir2.nodes()[0].name());
        assertTrue(dir2 == freg.getNodeDirectory(dir2.nodes()[0]));
        Iterator<FileNode> files = dir2.files();
        assertTrue(files.hasNext());
        FileNode nd2 = files.next();
        assertFalse(files.hasNext());
        assertTrue(nd2 == nd);
        assertTrue(dir2 == freg.getNodeDirectory(nd));

        // relative path into an existing directory
        path = "up" + SC + "we" + SC + "go.txt";
        nd = lfs.nodeFromString(path);
        assertNotNull(nd.parent());
        assertNotNull(nd.parent().parent());
        assertNull(nd.parent().parent().parent());
        dir = freg.root().dirs().next().dirs().next();
        assertTrue(freg.add(nodeToList(nd), null, dir, this));
        dir2 = freg.root().dirs().next().dirs().next();
        assertTrue(dir == dir2);
        dirs = dir.dirs();
        dir = dirs.next();
        assertTrue(dir.nodes()[0].name.equals("up"));
        assertFalse(dirs.hasNext());
        dirs = dir.dirs();
        dir = dirs.next();
        assertTrue(dir.nodes()[0].name.equals("we"));
        assertFalse(dirs.hasNext());
        assertFalse(dir.dirs().hasNext());
        files = dir.files();
        assertTrue(files.next().name().equals("go.txt"));
        assertFalse(files.hasNext());

        // absolute path with path collision
        assertTrue(lfs.roots().hasNext());
        FileNode firstRoot = lfs.roots().next();
        path = firstRoot.name() + "a" + SC + "b.txt";
        nd = lfs.nodeFromString(path);
        assertNotNull(nd.parent());
        assertNotNull(nd.parent().parent());
        assertNull(nd.parent().parent().parent());
        assertTrue(nd.parent().parent().hasAttributes(FileNode.ATTR_ROOT));
        this.merge = FileRegistrar.Callback.Merge.IGNORE;
        assertTrue(freg.add(nodeToList(nd), null, null, this));
        dirs = freg.root().dirs();
        assertTrue(dirs.hasNext());
        dir = dirs.next();
        assertFalse(dirs.hasNext());
        assertEquals("a", dir.nodes()[0].name());
        assertTrue(2 == dir.nodes().length);
        assertEquals("a", dir.nodes()[1].name());
        assertTrue(dir.nodes()[1] != dir.nodes()[0]);
        assertTrue(dir.nodes()[1] == nd.parent());
        files = dir.files();
        assertTrue(files.hasNext());
        assertTrue(dir.dirs().hasNext());   // don't go further up
        nd2 = dir.files().next();
        assertEquals("b.txt", nd2.name);
        assertTrue(nd2 == nd);

        // file collision (we need to use a relative path for that to be different)
        path = "a" + SC + "b.txt";
        nd = lfs.nodeFromString(path);
        this.merge = FileRegistrar.Callback.Merge.ABORT;
        assertFalse(freg.add(nodeToList(nd), null, null, this));
        dirs = freg.root().dirs();
        assertTrue(dirs.hasNext());
        dir = dirs.next();
        assertFalse(dirs.hasNext());
        assertTrue(2 == dir.nodes().length);
        this.merge = FileRegistrar.Callback.Merge.REPLACE;
        assertTrue(freg.add(nodeToList(nd), null, null, this));
        dirs = freg.root().dirs();
        assertTrue(dirs.hasNext());
        dir = dirs.next();
        assertFalse(dirs.hasNext());
        assertTrue(3 == dir.nodes().length);
        assertTrue(dir.nodes()[0] == nd.parent());
        this.merge = FileRegistrar.Callback.Merge.IGNORE;
        assertTrue(freg.add(nodeToList(nd), null, null, this));
        dirs = freg.root().dirs();
        assertTrue(dirs.hasNext());
        dir = dirs.next();
        assertFalse(dirs.hasNext());
        assertEquals("a", dir.nodes()[0].name());
        files = dir.files();
        assertTrue(files.hasNext());
        assertTrue(nd == dir.files().next());

        // try file in root
        assertFalse(freg.root().files().hasNext());
        path = "c.txt";
        nd = lfs.nodeFromString(path);
        this.merge = FileRegistrar.Callback.Merge.ABORT;
        assertTrue(freg.add(nodeToList(nd), null, null, this));
        this.merge = FileRegistrar.Callback.Merge.ABORT;
        assertFalse(freg.add(nodeToList(nd), null, null, this));
        this.merge = FileRegistrar.Callback.Merge.REPLACE;
        assertTrue(freg.add(nodeToList(nd), null, null, this));
        this.merge = FileRegistrar.Callback.Merge.IGNORE;
        assertTrue(freg.add(nodeToList(nd), null, null, this));
        files = freg.root().files();
        assertTrue(files.hasNext());
        assertTrue(nd == files.next());
        assertFalse(files.hasNext());

        // check for path limitation
        path = "x" + SC + "y" + SC + "z.txt";
        nd = lfs.nodeFromString(path);
        assertNotNull(nd.parent());
        assertNotNull(nd.parent().parent());
        assertNull(nd.parent().parent().parent());
        nd2 = nd.parent();
        this.merge = FileRegistrar.Callback.Merge.ABORT;
        assertTrue (freg.add(nodeToList(nd), nd2, null, this));
        assertFalse(freg.add(nodeToList(nd), nd2, null, this));
        dirs = freg.root().dirs();
        dir = null;
        int i = 0;
        for (; dirs.hasNext(); i++) {
            dir2 = dirs.next();
            if (dir2.nodes()[0].name.equals("y")) {
                assertNull(dir);
                dir = dir2;
            }
        }
        assertTrue(2 == i);
        assertTrue(null != dir && (files = dir.files()).hasNext());
        nd = files.next();
        assertTrue(nd.name.equals("z.txt"));

        // check for root exclusion
        path = "root_this_is" + SC + "l" + SC + "m.txt";
        nd = lfs.nodeFromString(path);
        assertNotNull(nd.parent());
        assertNotNull(nd.parent().parent());
        assertNull(nd.parent().parent().parent());
        nd.parent().parent().attributes |= FileNode.ATTR_ROOT;
        nd2 = nd.parent();
        this.merge = FileRegistrar.Callback.Merge.ABORT;
        assertTrue (freg.add(nodeToList(nd), null, null, this));
        assertFalse(freg.add(nodeToList(nd), null, null, this));
        dirs = freg.root().dirs();
        dir = null;
        i = 0;
        for (; dirs.hasNext(); i++) {
            dir2 = dirs.next();
            if (dir2.nodes()[0].name.equals("l")) {
                assertNull(dir);
                dir = dir2;
            }
        }
        assertTrue(3 == i);
        assertTrue(null != dir && (files = dir.files()).hasNext());
        nd = files.next();
        assertTrue(nd.name.equals("m.txt"));

        // add a sole file to different spots
        path = "root_this_is" + SC + "ignored" + SC + "sole.txt";
        nd = lfs.nodeFromString(path);
        assertNotNull(nd.parent());
        this.merge = FileRegistrar.Callback.Merge.ABORT;
        assertTrue (freg.add(nodeToList(nd), nd, null, this));
        assertFalse(freg.add(nodeToList(nd), nd, null, this));
        dir = freg.root().dirs().next();
        assertTrue (freg.add(nodeToList(nd), nd, dir, this));
        assertFalse(freg.add(nodeToList(nd), nd, dir, this));
        assertTrue(dir == freg.getNodeDirectory(nd));
        for (FileRegistrar.Directory dir3 : new
             FileRegistrar.Directory[] { dir, freg.root() } ) {
            files = dir3.files();
            while(files.hasNext()) {
                nd = files.next();
                if ("sole.txt".equals(nd.name())) {
                    files = null;
                    break;
                }
            }
            assertNull(files);
        }

        //FileRegistrar.dump(freg.root(), 0, System.out);
    }

    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void testVirtual() throws IOException {
        this.merge = FileRegistrar.Callback.Merge.IGNORE;

        FileRegistrar freg = new FileRegistrar.InMemory(new DefCmp(false));

        FileNode d0 = new FileNode.Virtual(null, "d0", -1L, 0x123, FileNode.ATTR_DIRECTORY);
        FileNode d1 = new FileNode.Virtual(d0  , "d1", -1L, 0x456, FileNode.ATTR_DIRECTORY);
        FileNode f0 = new FileNode.Virtual(d1  , "f0", -1L, 0x789, FileNode.ATTR_NONE);
        FileNode f1 = new FileNode.Virtual(d1  , "f1", -1L, 0xabc, FileNode.ATTR_NONE);

        List<FileNode> toadd = new ArrayList<>();
        toadd.add(f0);
        assertTrue(freg.add(toadd, null, null, this));

        toadd.clear();
        toadd.add(f1);
        assertTrue(freg.add(toadd, null, null, this));

        //FileRegistrar.dump(freg.root(), 0, System.out);

        assertFalse(freg.root().files().hasNext());
        Iterator<FileRegistrar.Directory> dirs = freg.root().dirs();
        assertTrue(dirs.hasNext());
        FileRegistrar.Directory dir = dirs.next();
        assertTrue("d0".equals(dir.nodes()[0].name()));
        assertFalse(dirs.hasNext());
        assertFalse(dir.files().hasNext());
        dirs = dir.dirs();
        assertTrue(dirs.hasNext());
        dir = dirs.next();
        assertFalse(dirs.hasNext());
        assertTrue("d1".equals(dir.nodes()[0].name()));
        assertFalse(dir.dirs().hasNext());
        Iterator<FileNode> files = dir.files();
        Map<String, FileNode> map = new HashMap<>();
        for (int i = 0; i < 2; i++) {
            assertTrue(files.hasNext());
            FileNode fn = files.next();
            map.put(fn.name(), fn);
        }
        assertFalse(files.hasNext());
        assertTrue(2 == map.size());
        assertTrue(map.get("f0").timestamp() == 0x789L);
        assertTrue(map.get("f1").timestamp() == 0xabcL);
    }

    ///////////////////////////////////////////////////////////////////////////

    FileRegistrar.Callback.Merge merge =
    FileRegistrar.Callback.Merge.ABORT;

    public Merge onMerge(FileNode[] nd0, FileNode nd1) {
        return this.merge;
    }

    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void testNamedRootNodeInclusionIssue() throws IOException {
        DbgFileSystem dfs = new DbgFileSystem(false, '|');

        dfs.addRoot("R");

        FileNode fn = dfs.nodeFromString("R|");
        assertTrue(fn.hasAttributes(FileNode.ATTR_DIRECTORY));

        FileRegistrar freg = new FileRegistrar.InMemory(new DefCmp(false));

        final int res = FileRegistrar.bulk(
            freg,
            fn,
            null,
            null,
            new FileRegistrar.BulkCallbacks() {
                public boolean onProgress(FileNode current) {
                    return true;
                }
                public Merge onMerge(FileNode[] nd0, FileNode nd1) {
                    return Merge.ABORT;
                }
                public boolean matches(FileNode file) {
                    return true;
                }
            },
            true,
            true);

        assertTrue(0 == res);

        FileRegistrar.dump(freg.root(), 0, System.out);

        assertFalse(freg.root().dirs().hasNext());
        assertFalse(freg.root().files().hasNext());
    }

    ///////////////////////////////////////////////////////////////////////////

    static FileRegistrar makeFileReg() throws IOException {
        DbgFileSystem dfs = new DbgFileSystem(false, '/');
        dfs.addRoot("R");
        dfs.createFile(null   , new String[] { "R", "a" },  0, 1234, 0, true);
        dfs.createFile("a.txt", new String[] { "R",     }, 10, 5555, 0, true);
        dfs.createFile("b.txt", new String[] { "R", "b" }, 20, 9999, 0, true);

        FileNode fn = dfs.nodeFromString("R/");
        assertTrue(fn.hasAttributes(FileNode.ATTR_DIRECTORY));

        FileRegistrar result = new FileRegistrar.InMemory(new DefCmp(false));

        assertTrue(2 == FileRegistrar.bulk(result, fn, null, null,
            new FileRegistrar.BulkCallbacks() {
                public boolean onProgress(FileNode current            ) { return true; }
                public Merge   onMerge   (FileNode[] nd0, FileNode nd1) { fail(); return Merge.ABORT; }
                public boolean matches   (FileNode file               ) { return true; }
            },
            true, true));

        //FileRegistrar.dump(freg.root(), 0, System.out);
        return result;
    }

    @Test
    public void testWalking() throws IOException {
        FileRegistrar freg = makeFileReg();

        final Set<String> expected = new HashSet<>();

        expected.add("R/a");
        expected.add("R/b/b.txt");
        expected.add("R/a.txt");
        FileRegistrar.walk(freg.root(), fn -> {
            assertNotNull(expected.remove(fn[0].path(true)));
            return true;
        }, true, true);
        assertTrue(0 == expected.size());

        expected.add("R/a");
        expected.add("R/b");
        expected.add("R/b/b.txt");
        expected.add("R/a.txt");
        FileRegistrar.walk(freg.root(), fn -> {
            assertNotNull(expected.remove(fn[0].path(true)));
            return true;
        }, true, false);
        assertTrue(0 == expected.size());

        expected.add("R/a");
        expected.add("R/b");
        expected.add("R/a.txt");
        FileRegistrar.walk(freg.root(), fn -> {
            assertNotNull(expected.remove(fn[0].path(true)));
            return true;
        }, false, false);
        assertTrue(2 == expected.size());
        expected.clear();

        expected.add("R/a");        // odd case, but still...
        expected.add("R/a.txt");
        FileRegistrar.walk(freg.root(), fn -> {
            assertNotNull(expected.remove(fn[0].path(true)));
            return true;
        }, false, true);
        assertTrue(1 == expected.size());
    }

    @Test
    public void testNodePath() throws IOException {
        FileRegistrar freg = makeFileReg();

        FileNode fn = freg.root().files().next();
        assertEquals(fn.name(), "a.txt");
        assertEquals(FileRegistrar.nodePath(fn), "a.txt");

        Iterator<Directory> id = freg.root().dirs();
        int c = 0, b = 0;
        while (id.hasNext()) {
            c++;
            Directory dir = id.next();
            String np = FileRegistrar.nodePath(dir.nodes()[0]);
            assertEquals(np, dir.nodes()[0].name() + "/");
            if (dir.nodes()[0].name().equals("b")) {
                fn = dir.files().next();
                assertEquals(fn.name(), "b.txt");
                assertEquals(FileRegistrar.nodePath(fn), "b/b.txt");
                b++;
            }
        }
        assertTrue(2 == c);
        assertTrue(1 == b);

        DbgFileSystem dfs = new DbgFileSystem(false, '/');
        dfs.addRoot("Z");
        dfs.createFile("z.txt", new String[] { "Z" },  1, 1, 0, true);
        fn = dfs.nodeFromString("Z/z.txt");
        assertNotNull(fn);
        assertNull(FileRegistrar.nodePath(fn));
    }
}
