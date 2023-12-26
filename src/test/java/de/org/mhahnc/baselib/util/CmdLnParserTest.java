package de.org.mhahnc.baselib.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class CmdLnParserTest {
    static class PropStrValidated extends Prp.Str {
        public PropStrValidated(String name, String dflt) {
            super(name, dflt);
        }
        public boolean validate(String raw) {
            return super.validate(raw) && !raw.equals("fail");
        }
    }

    @Test
    public void test0() throws Exception {
        CmdLnParser ap = new CmdLnParser();

        assertTrue(ap.parse(new String[0], false, false).length == 0);
        assertTrue(0 == ap.options().size());
        assertTrue(0 == ap.params ().size());

        assertTrue(ap.parse(new String[] { "abc" }, false, false).length == 0);
        assertTrue(0 == ap.options().size());
        assertTrue(1 == ap.params ().size());
        assertTrue(ap.params().get(0).equals("abc"));

        assertTrue(ap.parse(new String[] { "xyz", "", "117" }, false, false).length == 0);
        assertTrue(0 == ap.options().size());
        assertTrue(3 == ap.params ().size());
        assertTrue(ap.params().get(0).equals("xyz"));
        assertTrue(ap.params().get(1).equals(""   ));
        assertTrue(ap.params().get(2).equals("117"));

        ap.addProp("-x", new Prp.Bool("the.x", false));

        assertTrue(ap.parse(new String[0], false, false).length == 0);
        assertTrue(0 == ap.options().size());
        assertTrue(0 == ap.params ().size());

        assertTrue(ap.parse(new String[] { "-x" }, false, false).length == 0);
        assertTrue(1 == ap.options().size());
        assertEquals(Boolean.TRUE.toString(), ap.options().getProperty("the.x"));
        assertTrue(0 == ap.params().size());

        ap.addProp("-uuu"   , new PropStrValidated("the.s", "something"));
        ap.addProp("--d-e-e", new Prp.Str("d.e.e", "anything" ));

        assertTrue(ap.parse(new String[] { "param0", "-x=false", "p1", "---x", "----Y", "-uuu=very special", "prm2", "--d-e-e=--d-e-e" }, false, false).length == 0);
        assertTrue(3 == ap.options().size());
        assertEquals(Boolean.FALSE.toString(), ap.options().getProperty("the.x"));
        assertEquals("very special", ap.options().getProperty("the.s"));
        assertEquals("--d-e-e"     , ap.options().getProperty("d.e.e"));
        assertTrue(5 == ap.params().size());
        assertTrue(ap.params().get(0).equals("param0"));
        assertTrue(ap.params().get(1).equals("p1"));
        assertTrue(ap.params().get(2).equals("-x"));
        assertTrue(ap.params().get(3).equals("--Y"));
        assertTrue(ap.params().get(4).equals("prm2"));

        assertTrue(ap.parse(new String[] { "abc", "-"    }, false, false).length == 1);
        assertTrue(ap.parse(new String[] { "abc", "--"   }, false, false).length == 1);
        assertTrue(ap.parse(new String[] { "abc", "-i=9" }, false, false)[0].equals("-i=9"));

        String[] res = ap.parse(new String[] { "abc", "--", "" }, true, true);
        assertTrue(res.length == 2);
        assertTrue(ap.params().size() == 0);
        assertEquals(res[0], "abc");
        assertEquals(res[1], "--");
        assertTrue(ap.parse(new String[] { "abc", "--", "" }, true, false).length == 3);

        try { ap.parse(new String[] { "abc", "-uuu"      }, false, false); fail(); } catch (CmdLnParser.Error ape) { assertEquals(ape.getMessage(), "-uuu"     ); };
        try { ap.parse(new String[] { "abc", "-uuu=fail" }, false, false); fail(); } catch (CmdLnParser.Error ape) { assertEquals(ape.getMessage(), "-uuu=fail"); };
    }
}
