package de.org.mhahnc.baselib.io;

import java.util.Random;

import org.junit.Test;

import de.org.mhahnc.baselib.util.Clock;
import de.org.mhahnc.baselib.util.VarRef;

import static org.junit.Assert.assertTrue;

public class DefaultFileSystemFilterTest {
    @Test
    public void test0() {
        final String MATCH   = Boolean.TRUE.toString();
        final String NOMATCH = Boolean.FALSE.toString();
        for (final String[] m : new String[][] {
                { "*"         , "abc.txt"   , MATCH },
                { "*"         , ""          , MATCH },
                { "?"         , ""          , NOMATCH },
                { "?"         , "1"         , MATCH },
                { "?"         , "12"        , NOMATCH },
                { "*.txt"     , "abc.txt"   , MATCH },
                { "*txt"      , "1.txt"     , MATCH },
                { "*txt"      , "1txtt"     , NOMATCH },
                { "1?2?3.txt" , "1x2y3.txt" , MATCH },
                { "1?2?3.txt" , "1x2y3t.txt", NOMATCH },
                { "*txt"      , "1txt"      , MATCH },
                { "*txt"      , "1txt"      , MATCH },
                { "*txt"      , "1txt"      , MATCH },
                { "?ab*c.??"  , "xabYZc.12" , MATCH },
                { "?ab*c.??"  , "xabYZc.123", NOMATCH },
                { "?ab*c.??"  , "xabYc.123" , NOMATCH },
                { "?ab*c.??"  , "xabc.12"   , MATCH },
                { "?ab*c.??"  , "xxabc.123" , NOMATCH },
        }) {
            assertTrue(DefaultFileSystemFilter.isMask(m[0]));

            FileSystem.Filter fsf = new DefaultFileSystemFilter(m[0]);

            FileNode fn = new FileNode() {
                {
                    this.name = m[1];
                }
                public FileSystem fileSystem() {
                    return null;
                }
            };

            boolean m2 = fsf.matches(fn);

            assertTrue(Boolean.parseBoolean(m[2]) == m2);
        }
    }

    @Test
    public void testPerformance() {
        final Random rnd = new Random(0x88997733);

        FileSystem.Filter fsf = new DefaultFileSystemFilter("?ab*c??x*");

        final VarRef<String> nm = new VarRef<>();

        final FileNode fn = new FileNode() {
            @Override
            public String name() {
                return (this.name = nm.v);
            }
            public FileSystem fileSystem() {
                return null;
            }
        };

        long tm = 0L, start = Clock._system.now();

        final char[] buf = new char[24];

        final int LOOPS = 12345;
        final int MILLIS = de.org.mhahnc.baselib.test.Control.quick() ? 500 : 5000;

        long ops = 0;
        long matches = 0;
        while (tm < MILLIS) {
            for (int i = 0; i < LOOPS; i++) {
                for (int j = 0, d = buf.length; j < d; j++) {
                    buf[j] = (char)('a' + ((rnd.nextInt() & 0x7fffffff) % ('z' - 'a' + 1)));
                }
                nm.v = new String(buf);
                matches += fsf.matches(fn) ? 1L : 0L;
            }
            ops += LOOPS;
            tm = Clock._system.now() - start;
        }
        assertTrue(ops > 0);
        assertTrue(matches > 0);

        System.out.printf("matches: %d, tm: %d, ops: %d, %.0f ops/sec",
                matches, tm, ops, (ops * 1000.0) / tm);
    }
}
