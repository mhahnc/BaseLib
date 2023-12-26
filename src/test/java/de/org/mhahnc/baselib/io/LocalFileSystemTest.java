package de.org.mhahnc.baselib.io;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import org.junit.After;
import org.junit.Test;

import de.org.mhahnc.baselib.test.util.TestUtils;
import de.org.mhahnc.baselib.util.BinUtils;

public class LocalFileSystemTest {
    @After
    public void tearDown() {
        IOUtils.__test_CURRENTPATH = null;
    }

    @Test
    public void test0() throws IOException {
        final String DIR_1 = "111";
        final String DIR_2 = "222";
        final String FILE_NAME = "test.txt";
        final byte[] TEST_DATA = "some text.".getBytes();

        File tmpdir = new File(System.getProperty("java.io.tmpdir"));
        File path = new File(new File(tmpdir, DIR_1), DIR_2);
        File fl = new File(path, FILE_NAME);
        assertFalse(path.exists());

        try {
            assertTrue(path.mkdirs());

            FileOutputStream fos = new FileOutputStream(fl);
            fos.write(TEST_DATA);
            fos.close();

            String fpath = fl.getAbsolutePath();

            FileSystem fs = new LocalFileSystem(false);

            FileNode fnode = fs.nodeFromString(fpath);
            assertNotNull(fnode);
            FileNode walk = fnode;
            assertEquals(FILE_NAME, walk.name());
            assertTrue(!walk.hasAttributes(FileNode.ATTR_DIRECTORY));
            assertTrue(!walk.hasAttributes(FileNode.ATTR_ROOT));
            assertTrue(TEST_DATA.length == walk.size);
            assertTrue(0 != walk.timestamp);

            FileNode pnode = walk = walk.parent();
            assertNotNull(walk);
            assertEquals(DIR_2, walk.name());
            assertTrue( walk.hasAttributes(FileNode.ATTR_DIRECTORY));
            assertTrue(!walk.hasAttributes(FileNode.ATTR_ROOT));

            walk = walk.parent();
            assertNotNull(walk);
            assertEquals(DIR_1, walk.name());
            assertTrue( walk.hasAttributes(FileNode.ATTR_DIRECTORY));
            assertTrue(!walk.hasAttributes(FileNode.ATTR_ROOT));

            walk = walk.parent();
            while (null != walk) {
                assertTrue(null == walk.parent() ?
                            walk.hasAttributes(FileNode.ATTR_ROOT) :
                           !walk.hasAttributes(FileNode.ATTR_ROOT));

                assertTrue(walk.hasAttributes(FileNode.ATTR_DIRECTORY));

                // NOTE: under MacOS directory seems to be able to have sizes
                //       larger than zero (one candidate was "-Tmp" in the
                //       Java temporary path, which has been moved recently
                //       to a more obscure format as it seems)
                //assertTrue(0 == walk.size());

                walk = walk.parent();
            }

            Iterator<FileNode> i = fs.list(pnode, null);
            assertTrue(i.hasNext());
            FileNode fnode2 = i.next();
            assertFalse(i.hasNext());
            assertFalse(fnode2.hasAttributes(FileNode.ATTR_ROOT));

            // TODO: test with filter (no files returned)

            InputStream ins = fs.openRead(fnode2);
            byte[] data = IOUtils.readStreamBytes(ins);
            assertNotNull(data);
            assertTrue(BinUtils.arraysEquals(data, TEST_DATA));
        }
        finally {
            assertTrue(!fl  .exists() || fl  .delete());
            assertTrue(!path.exists() || path.delete());
            assertTrue(!path.getParentFile().exists() ||
                        path.getParentFile().delete());
        }
    }

    @Test
    public void testLinking() throws IOException {
        File tmpDir = TestUtils.createTempDir("LocalFileSystemTest.testLinking");
        assertTrue(tmpDir.exists());

        File currDir, adir;
        currDir = new File(tmpDir , "a"); adir = currDir;
        currDir = new File(currDir, "b");
        currDir = new File(currDir, "c");

        assertTrue(currDir.mkdirs() && currDir.exists());

        String r = "../../b/./c/../../x/y.txt".replace('/', File.separatorChar);
        String f = "x/y.txt".replace('/', File.separatorChar);

        String[] rrp = IOUtils.resolveRelativePath(new File(r), currDir);

        assertEquals(2, rrp.length);
        assertEquals(rrp[0], adir.getPath());
        assertEquals(rrp[1], f);

        IOUtils.__test_CURRENTPATH = currDir.getAbsolutePath();

        LocalFileSystem lfs = new LocalFileSystem(false);

        FileNode fn = lfs.nodeFromString(r);

        assertEquals(fn.path(false), f);
        assertEquals(fn.path(true), new File(adir, f).getAbsolutePath());

        r = "..";
        File dir = currDir;
        for (;;) {
            r += File.separatorChar;
            r += "..";
            if (null == (dir = dir.getParentFile())) {
                break;
            }
        }
        rrp = IOUtils.resolveRelativePath(new File(r), currDir);
        String root = IOUtils.getRoot(currDir).getPath();
        assertTrue(2 == rrp.length);
        assertEquals(rrp[0], root);
        fn = lfs.nodeFromString(r);
        assertEquals(root, fn.path(true));
        assertEquals(""  , fn.path(false));

        assertTrue(TestUtils.removeDir(tmpDir, true));
    }

    @Test
    public void testCurrentPath() throws IOException {
        File tmpDir = TestUtils.createTempDir("LocalFileSystemTest.testCurrentPath");

        final String currPath = tmpDir.getAbsolutePath();
        IOUtils.__test_CURRENTPATH = currPath;

        LocalFileSystem lfs = new LocalFileSystem(false);

        FileNode fn = lfs.nodeFromString(".");

        assertNull(fn.name());
        assertEquals(fn.link(), currPath);

        assertTrue(TestUtils.removeDir(tmpDir, true));
    }
}
