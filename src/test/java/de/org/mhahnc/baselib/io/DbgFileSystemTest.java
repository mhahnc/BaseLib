package de.org.mhahnc.baselib.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import org.junit.Test;

import de.org.mhahnc.baselib.test.util.TestUtils;
import de.org.mhahnc.baselib.util.BinUtils;
import de.org.mhahnc.baselib.util.MiscUtils;

public class DbgFileSystemTest {
    @Test
    public void test0() throws IOException {
        DbgFileSystem dfs = new DbgFileSystem(true, null);
        assertFalse(dfs.roots().hasNext());
        assertNull(dfs.nodeFromString("//not/there.yet"));

        final long FSIZE = 1001;
        for (int i = 0; i < 2; i++) {
            assertTrue(i > 0 ^ (null != dfs.createFile(
                "test.txt",
                new String[] { "C", "docs" },
                FSIZE,
                System.currentTimeMillis(),
                0,
                false)));
        }

        Iterator<FileNode> i = dfs.roots();
        assertTrue(i.hasNext());
        FileNode fn = i.next();
        assertFalse(i.hasNext());
        assertTrue(fn.hasAttributes(FileNode.ATTR_ROOT));
        assertTrue(fn.hasAttributes(FileNode.ATTR_DIRECTORY));
        assertTrue("C".equals(fn.name()));

        i = dfs.list(fn, FileSystem.Filter.MATCHALL);
        assertTrue(i.hasNext());
        fn = i.next();
        assertFalse(i.hasNext());
        assertFalse(fn.hasAttributes(FileNode.ATTR_ROOT));
        assertTrue (fn.hasAttributes(FileNode.ATTR_DIRECTORY));
        assertTrue (fn.timestamp() == 0);
        assertTrue("docs".equals(fn.name()));
        try {
            dfs.openRead(fn);   // can't open a directory
        }
        catch (IOException expected) {
            assertNotNull(expected.getMessage());
        }

        i = dfs.list(fn, FileSystem.Filter.MATCHALL);
        assertTrue(i.hasNext());
        fn = i.next();
        assertFalse(i.hasNext());
        assertFalse(fn.hasAttributes(FileNode.ATTR_ROOT));
        assertFalse(fn.hasAttributes(FileNode.ATTR_DIRECTORY));
        assertTrue (fn.size() == FSIZE);
        assertNull (fn.getTag("something"));
        assertTrue("test.txt".equals(fn.name()));

        byte[] data0 = MiscUtils.readInputStream(dfs.openRead(fn));
        assertTrue(FSIZE == data0.length);

        String path = fn.path(false);
        FileNode fn2 = dfs.nodeFromString(path);
        assertNotNull(fn2);
        assertTrue(dfs.areNodesEqual(fn, fn2, true, true));
        assertTrue(fn == fn2);

        byte[] data1 = MiscUtils.readInputStream(dfs.openRead(fn2));
        assertTrue(FSIZE == data1.length);

        assertTrue(BinUtils.arraysEquals(data0, data1));

        Stack<String> path2 = new Stack<>();
        path2.push("D");
        final int DEPTH_MAX = 51;
        final int NUM_OF_FILES = 2;
        final String DIRPFX = "depth";
        int len = 0, depth = 0;
        for (int inc = 0; depth < DEPTH_MAX; depth++) {
            for (int num = 0; num < NUM_OF_FILES; num++) {
                assertNotNull(dfs.createFile(
                    String.format("file%d.%d", len, num),
                    path2.toArray(new String[0]),
                    len,
                    System.currentTimeMillis(),
                    0,
                    false));
            }

            len += (len >> 2) + ++inc;
            path2.push(DIRPFX + depth);
        }

        path2.push(DIRPFX + depth);
        assertNotNull(dfs.createFile(
                null,
                path2.toArray(new String[0]),
                0, 0, 0, false));

        i = dfs.roots();
        FileNode dir = null;
        while (i.hasNext()) {
            fn = i.next();
            if ("D".equals(fn.name())) {
                dir = fn;
                break;
            }
        }
        assertNotNull(fn);
        depth = 0;
        int fsz = 0;
        for (int inc = 0; depth < DEPTH_MAX; fsz += (fsz >> 2) + ++inc) {
            i = dfs.list(dir, FileSystem.Filter.MATCHALL);
            List<FileNode> items = TestUtils.itrToLst(i, true);
            assertNotNull(items);
            assertTrue(1 + NUM_OF_FILES == items.size());
            dir = null;
            for (FileNode item : items) {
                if (item.hasAttributes(FileNode.ATTR_DIRECTORY)) {
                    items.remove(dir = item);
                    break;
                }
            }
            assertNotNull(dir);
            assertTrue(NUM_OF_FILES == items.size());
            assertTrue(dir.name().equals(String.format("depth%d", depth++)));

            Collections.sort(items, (fn1_, fn2_) -> fn1_.name().compareTo(fn2_.name));

            byte[][] datas = new byte[NUM_OF_FILES][];
            for (int j = 0; j < NUM_OF_FILES; j++) {
                fn = items.get(j);
                assertFalse(fn.hasAttributes(FileNode.ATTR_DIRECTORY));
                assertFalse(fn.hasAttributes(FileNode.ATTR_ROOT));
                assertFalse(fn.hasAttributes(FileNode.ATTR_DIRECTORY));
                assertTrue (fn.size() == fsz);
                assertNull (fn.getTag("anything"));
                assertTrue(("file" + fsz + "." + j).equals(fn.name()));
                data0 = MiscUtils.readInputStream(dfs.openRead(fn));
                assertTrue(data0.length == fsz);
                fn2 = dfs.nodeFromString(fn.path(false));
                assertNotNull(fn2);
                data1 = MiscUtils.readInputStream(dfs.openRead(fn));
                assertTrue(BinUtils.arraysEquals(data0, data1));
                datas[j] = data1;
            }
            assertTrue (datas[0].length == datas[1].length);
            assertFalse(0 < fsz && BinUtils.arraysEquals(datas[0], datas[1]));
        }
        assertTrue(fsz == len);

        assertTrue(dfs.addRoot(new String("root2".toCharArray())));
        int root2 = 0;
        len = 0;
        i = dfs.roots();
        while (i.hasNext()) {
            fn = i.next();
            assertTrue(fn.hasAttributes(FileNode.ATTR_ROOT));
            if (fn.name().equals("root2")) {
                root2++;
            }
            len++;
        }
        assertTrue(1 == root2);
        assertTrue(3 == len);
    }

    @Test
    public void testPerformance() throws IOException {
        final long TEST_MILLIS = de.org.mhahnc.baselib.test.Control.quick() ? 1000L : 5000L;

        DbgFileSystem dfs = new DbgFileSystem(false, null);

        FileNode fn = dfs.createFile("biggg.dat",
                new String[] { "root" },
                Long.MAX_VALUE,
                1L,
                FileNode.ATTR_NONE,
                false);

        assertNotNull(fn);

        java.io.InputStream ins = dfs.openRead(fn);
        final byte[] buf = new byte[DbgFileSystem.MAX_NORND_READ_SZ];

        long tm = System.currentTimeMillis();
        long total = 0;
        while (System.currentTimeMillis() - tm < TEST_MILLIS) {
            final int read = ins.read(buf);
            assertTrue(buf.length == read);
            total += read;
        }
        tm = System.currentTimeMillis() - tm;

        System.out.printf("%,d bytes per second\n", (total * 1000L) / tm);
    }
}
