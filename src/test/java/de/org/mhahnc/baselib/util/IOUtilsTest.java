package de.org.mhahnc.baselib.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Test;

import de.org.mhahnc.baselib.io.IOUtils;
import de.org.mhahnc.baselib.test.util.TestUtils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

public class IOUtilsTest extends TestUtils {
    @Test
    public void testReadStreamBytes() throws IOException {
        byte[] data = new byte[10000];
        fillPattern123(data, 0, data.length);

        byte[] data2 = IOUtils.readStreamBytes(new ByteArrayInputStream(data));

        assertTrue(BinUtils.arraysEquals(data, data2));
        assertTrue(checkPattern123(data2, 0, data2.length));
    }

    @Test
    public void testGetRoot() {
        File[] roots = File.listRoots();
        assertTrue(null != roots && 0 < roots.length);

        final File root = roots[0];

        File fl = new File(root, "xyz" + File.separator);
        assertTrue(root.equals(IOUtils.getRoot(fl)));

        fl = new File(root, "xyz" + File.separator + "abc.txt");
        assertTrue(root.equals(IOUtils.getRoot(fl)));

        assertTrue(root.equals(IOUtils.getRoot(root)));

        assertFalse(root.equals(IOUtils.getRoot(new File("123.txt"))));
    }

    @Test
    public void testGetFileElements() {
        List<String> elems = IOUtils.fileToElements(new File("a/b/c"));
        assertTrue(3 == elems.size());
        assertEquals("a", elems.get(0));
        assertEquals("b", elems.get(1));
        assertEquals("c", elems.get(2));
        assertEquals("a/b/c".replace('/', File.separatorChar),
                     IOUtils.elementsToFileStr(elems, 0, elems.size(), false));

        elems = IOUtils.fileToElements(new File("a"));
        assertTrue(1 == elems.size());
        assertEquals("a", elems.get(0));
        elems = IOUtils.fileToElements(new File("/a"));
        assertTrue(2 == elems.size());
        assertEquals("" , elems.get(0));
        assertEquals("a", elems.get(1));
        elems = IOUtils.fileToElements(new File("/"));
        assertTrue(1 == elems.size());
        assertEquals("", elems.get(0));
        elems = IOUtils.fileToElements(new File(""));
        assertTrue(1 == elems.size());
        assertEquals("", elems.get(0));
    }

    @Test
    public void testStripRoot() {
        File[] roots = File.listRoots();
        assertTrue(null != roots && 0 < roots.length);

        final File root = roots[0];

        for (String[] p : new String[][] {
            { "xyz" + File.separator, "xyz" },
            { "xyz", "xyz" },
            { "xyz" + File.separator + "123.txt", "xyz" + File.separator + "123.txt"  },
            { "", "" }
        }) {
            File fl = new File(root, p[0]);
            String sr = IOUtils.stripRoot(fl).toString();
            assertEquals(p[1], sr);
        }
    }

    @Test
    public void testResolveRelativePath() throws IOException {
        File ccdir  = createTempDir("IOUtilsTest.testResolveRelativePath");
        File ccdir2 = new File(ccdir , "ccdir2");
        File ccdir3 = new File(ccdir2, "ccdir3");
        assertTrue(ccdir3.mkdirs() && ccdir3.exists());

        for (String fname : new String[] { "", "123.txt" }) {
            String fe0 = "".equals(fname) ? null : npath(fname);
            String fe1 = "".equals(fname) ? "" : npath("/" + fname);

            String[] rrp = IOUtils.resolveRelativePath(new File(fname), ccdir);
            assertEquals(rrp[0], ccdir.getPath());
            assertEquals(rrp[1], fe0);

            File sdir = new File("somedir", fname);
            rrp = IOUtils.resolveRelativePath(sdir, ccdir);
            assertEquals(rrp[0], ccdir.getPath());
            assertEquals(rrp[1], sdir.getPath());

            sdir = new File(npath("./somedir"), fname);
            rrp = IOUtils.resolveRelativePath(sdir, ccdir);
            assertEquals(rrp[0], ccdir.getPath());
            assertEquals(rrp[1], "somedir" + fe1);

            rrp = IOUtils.resolveRelativePath(new File(npath("../" + fname)), ccdir2);
            assertEquals(rrp[0], ccdir.getPath());
            assertEquals(rrp[1], fe0);

            rrp = IOUtils.resolveRelativePath(new File(npath("../ccdir2/") + fname), ccdir2);
            assertEquals(rrp[0], ccdir.getPath());
            assertEquals(rrp[1], "ccdir2" + fe1);

            rrp = IOUtils.resolveRelativePath(new File(npath("../ccdir2/ccdir3/") + fname), ccdir2);
            assertEquals(rrp[0], ccdir.getPath());
            assertEquals(rrp[1], npath("ccdir2/ccdir3" + fe1));

            rrp = IOUtils.resolveRelativePath(new File(npath("../ccdir2/./ignored/../ccdir3/") + fname), ccdir2);
            assertEquals(rrp[0], ccdir.getPath());
            assertEquals(rrp[1], npath("ccdir2/ccdir3" + fe1));

            rrp = IOUtils.resolveRelativePath(new File(npath("ccdir2/ccdir3/x.txt")), ccdir);
            assertEquals(rrp[0], ccdir.getPath());
            assertEquals(rrp[1], npath("ccdir2/ccdir3/x.txt"));

            rrp = IOUtils.resolveRelativePath(new File(".", fname), ccdir);
            assertEquals(rrp[0], ccdir.getPath());
            assertEquals(rrp[1], fe0);

            String expr = "..";
            for (int i = 0, c = IOUtils.fileToElements(ccdir2).size(); i < c; i++) {
                expr += File.separator + "..";
            }
            expr += npath("/" + fname);
            rrp = IOUtils.resolveRelativePath(new File(expr), ccdir2);
            assertEquals(rrp[0], IOUtils.getRoot(ccdir2).getPath());
            assertEquals(rrp[1], fe0);
        }

        removeDir(ccdir, true);
    }


    @Test
    public void testExtractPath() {
        for (final String[] s : new String[][] {
                { "x"        , null  , null     },
                { "x/"       , null  , null     },
                { "x/x"      , null  , null     },
                { "x/*"      , "*"   , "x/"     },
                { "*"        , "*"   , null     },
                { "/*"       , "*"   , "/"      },
                { "????"     , "????", null     },
                { "a/b/c/?i*", "?i*" , "a/b/c/" }
        }) {
            String[] r = IOUtils.extractMask(npath(s[0]));
            if (null == s[1]) {
                assertNull(r);
                continue;
            }
            assertTrue(2 == r.length);
            if (null == s[2]) {
                assertNull(r[0]);
            }
            else {
                assertNotNull(r[0]);
                assertEquals(npath(s[2]), npath(r[0]));
            }
            assertEquals(npath(s[1]), npath(r[1]));
        }
    }
}
