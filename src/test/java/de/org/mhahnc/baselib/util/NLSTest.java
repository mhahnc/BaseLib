package de.org.mhahnc.baselib.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class NLSTest {
    @Before
    public void setUp() {
        NLS.Reg.instance().reset();
    }

    @After
    public void tearDown() {
        NLS.Reg.instance().reset();
    }

    @Test
    public void test0() throws Exception {
        try {
            final VarInt lc = new VarInt();
            NLS.Reg.Listener l = () ->lc.v++;
            NLS.Reg.instance().register(TestNLS.class);
            NLS.Reg nreg = NLS.Reg.instance();
            assertTrue (nreg.addListener(l));
            assertFalse(nreg.addListener(l));
            assertNull(nreg.id());
            assertTrue(lc.v == 0);
            nreg.load("de");
            assertNotNull(nreg.id());
            assertTrue(lc.v == 1);
            assertTrue (nreg.removeListener(l));
            assertFalse(nreg.removeListener(l));
            assertEquals(TestNLS.ANOTHER_RES           , "nochmal eine");
            assertEquals(TestNLS.HELLO                 , "hallÖ");
            assertEquals(TestNLS.FORMAT_THIS_1.fmt("1"), "bitte 1X formatieren");
            nreg.load("en");
            assertTrue(lc.v == 1);
            assertEquals(TestNLS.ANOTHER_RES           , "another resource string");
            assertEquals(TestNLS.HELLO                 , "hello");
            assertEquals(TestNLS.FORMAT_THIS_1.fmt("1"), "please format this 1 times");
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static class TestNLS extends NLS {
        public static Str HELLO;
        public static Str ANOTHER_RES;
        public static Str FORMAT_THIS_1;
    }
}
