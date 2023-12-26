package de.org.mhahnc.baselib.util;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class MiscUtilsTest {

    Locale defaultLocale;

    @Before
    public void setup() {
        this.defaultLocale = Locale.getDefault();
        Locale.setDefault(new Locale("en", "US"));
    }

    @After
    public void teardown() {
        Locale.setDefault(this.defaultLocale);
    }

    @Test
    public void testFillString() {
        assertTrue(""    .equals(MiscUtils.fillString(0, '\0')));
        assertTrue("a"   .equals(MiscUtils.fillString(1, 'a')));
        assertTrue("4444".equals(MiscUtils.fillString(4, '4')));
    }

    @Test
    public void testFillChars() {
        CharSequence cs = MiscUtils.fillChars(10, 'x');

        assertTrue("x"  .equals(cs.subSequence(0, 1)));
        assertTrue("xxx".equals(cs.subSequence(0, 3)));
        assertTrue("xxx".equals(cs.subSequence(7, 10)));
        try {
            cs.subSequence(0, 11);
            fail();
        }
        catch(StringIndexOutOfBoundsException sioobe) {
        }
    }

    @Test
    public void testCSV() {
        String[] l = MiscUtils.csvLoad("x", false);
        assertTrue(1 == l.length);
        assertTrue(l[0].equals("x"));
        l = MiscUtils.csvLoad("x, y ", false);
        assertTrue(2 == l.length);
        assertTrue(l[0].equals("x"));
        assertTrue(l[1].equals("y"));
        l = MiscUtils.csvLoad(" x ,y ,  zzz", false);
        assertTrue(3 == l.length);
        assertTrue(l[0].equals("x"));
        assertTrue(l[1].equals("y"));
        assertTrue(l[2].equals("zzz"));
        l = MiscUtils.csvLoad("", false);
        assertTrue(1 == l.length);
        assertTrue(l[0].equals(""));
        l = MiscUtils.csvLoad("", true);
        assertTrue(0 == l.length);
        l = MiscUtils.csvLoad(",", true);
        assertTrue(0 == l.length);
        l = MiscUtils.csvLoad("  , ,  ,", true);
        assertTrue(0 == l.length);

        assertTrue("a,bb,ccc".equals(MiscUtils.csvSave(new String[] { "a", " bb", " ccc " } )));
        assertTrue(",,"      .equals(MiscUtils.csvSave(new String[] { "  ", "  ", ""      } )));
        assertTrue("a"       .equals(MiscUtils.csvSave(new String[] { "a"                 } )));
    }

    @Test
    public void testReadFile() throws IOException {
        File tmp = new File(System.getProperty("java.io.tmpdir"),
                String.format("testReadFile%08x.dat", new SecureRandom().nextLong()));
        assertTrue(!tmp.exists() || tmp.delete() || !tmp.exists());
        final String TESTSTR = "this will better work";
        MiscUtils.writeFile(tmp, TESTSTR.getBytes());
        assertTrue(TESTSTR.equals(new String(MiscUtils.readFile(tmp))));
        assertTrue(tmp.delete());
    }

    @Test
    public void testUniqueRandomIndexes() {
        final Random rnd = new Random(0x12345678);

        for (int c : new int[] { 0, 1, 2, 3, 49, 50, 0xffff, 100000 }) {
            int[] uris = MiscUtils.uniqueRandomIndexes(c, rnd);

            assertTrue(c == uris.length);

            long sum = 0L;
            for (int uri : uris) {
                sum += uri;
            }

            long lc = c;
            long expected = (lc * (lc - 1L)) >>> 1;

            assertTrue(expected == sum);

            Arrays.sort(uris);
            for (int i = 0; i < uris.length; i++) {
                assertTrue(i == uris[i]);
            }
        }
    }

    @Test
    public void testFactoredStringToULong() {
        assertTrue(0                 == MiscUtils.factoredStringToULong("0"));
        assertTrue(1234567890000L    == MiscUtils.factoredStringToULong("1234567890000"));
        assertTrue(1000              == MiscUtils.factoredStringToULong("1K"));
        assertTrue(1 << 10           == MiscUtils.factoredStringToULong("1k"));
        assertTrue(1000000           == MiscUtils.factoredStringToULong("1M"));
        assertTrue(1 << 20           == MiscUtils.factoredStringToULong("1m"));
        assertTrue(1000000000        == MiscUtils.factoredStringToULong("1G"));
        assertTrue(1 << 30           == MiscUtils.factoredStringToULong("1g"));
        assertTrue(1000000000000L    == MiscUtils.factoredStringToULong("1T"));
        assertTrue(1L << 40          == MiscUtils.factoredStringToULong("1t"));
        assertTrue(1000000000000000L == MiscUtils.factoredStringToULong("1P"));
        assertTrue(1L << 50          == MiscUtils.factoredStringToULong("1p"));
        assertTrue(Long.MAX_VALUE    == MiscUtils.factoredStringToULong(Long.toString(Long.MAX_VALUE)));
        assertTrue(Long.MAX_VALUE    == MiscUtils.factoredStringToULong(Long.toString(Long.MAX_VALUE)));
        assertTrue(1001000000        == MiscUtils.factoredStringToULong("1001M"));

        for (final String bad : new String[] {
                "", "m", "100000m ", " 100", "24i", "10000000000000000000000000000000m"
        }) {
            assertNull(MiscUtils.factoredStringToULong(bad));
        }
    }

    @Test
    public void testStrToUSz() {
        assertTrue(0L                == MiscUtils.strToUSz("0"));
        assertTrue(1L                == MiscUtils.strToUSz("1 "));
        assertTrue(1234567898765431L == MiscUtils.strToUSz(" 1234567898765431  "));
        assertTrue(Long.MAX_VALUE    == MiscUtils.strToUSz(Long.toString(Long.MAX_VALUE)));
        assertTrue(1000L             == MiscUtils.strToUSz("1k"));
        assertTrue(1000L             == MiscUtils.strToUSz("1K"));
        assertTrue(123000L           == MiscUtils.strToUSz("123K"));
        assertTrue(1234000L          == MiscUtils.strToUSz(" 1234 k"));
        assertTrue(2000000L          == MiscUtils.strToUSz("2m"));
        assertTrue(23000000L         == MiscUtils.strToUSz(" 23 M"));
        assertTrue(2000000L          == MiscUtils.strToUSz("2m"));
        assertTrue(4000000000L       == MiscUtils.strToUSz("4g"));
        assertTrue(4000000000L       == MiscUtils.strToUSz("4G"));
        assertTrue(9000000000000L    == MiscUtils.strToUSz("9t"));
        assertTrue(8000000000000L    == MiscUtils.strToUSz(" 8 T"));
        assertTrue(3000000000000000L == MiscUtils.strToUSz("3p"));
        assertTrue(4000000000000000L == MiscUtils.strToUSz(" 4P"));
        assertTrue(1024L             == MiscUtils.strToUSz("1ki"));
        assertTrue(1024L             == MiscUtils.strToUSz("1KI"));
        assertTrue(1048576           == MiscUtils.strToUSz("1mi"));
        assertTrue(1048576           == MiscUtils.strToUSz("1mI"));
        assertTrue(1073741824        == MiscUtils.strToUSz("1gi"));
        assertTrue(1073741824        == MiscUtils.strToUSz("1GI"));
        assertTrue(1099511627776L    == MiscUtils.strToUSz("1ti"));
        assertTrue(1099511627776L    == MiscUtils.strToUSz(" 1 TI"));
        assertTrue(1125899906842624L == MiscUtils.strToUSz("1pi"));
        assertTrue(1125899906842624L == MiscUtils.strToUSz(" 1Pi"));

        assertTrue(-1L == MiscUtils.strToUSz(""));
        assertTrue(-1L == MiscUtils.strToUSz(" "));
        assertTrue(-2L == MiscUtils.strToUSz("1000000000000000000000000000000000000000000000000001"));
        assertTrue(-2L == MiscUtils.strToUSz("9200000000000000000000000000000000000000000000000000K"));
        assertTrue(-3L == MiscUtils.strToUSz("1kJ"));
        assertTrue(-3L == MiscUtils.strToUSz("1x"));
        assertTrue(-3L == MiscUtils.strToUSz("1k i"));
        assertTrue(-4L == MiscUtils.strToUSz(Long.toString(Long.MAX_VALUE / 1000L + 1) + "k"));

        for (String moreBad : new String[] {
                " \t", "iouha30", "4109843.97", "\r\n\n", "&*$!%#$!&*)(^",
                "1 2 3", "1 2", "309183I209", "987654321IM", "-1", "--22"
        }) {
            assertTrue(0L > MiscUtils.strToUSz(moreBad));
        }
    }

    static String d2s(String s) {
        return s.replace('.', DecimalFormatSymbols.getInstance().getDecimalSeparator());
    }

    @Test
    public void testUszToStr() {
        assertEquals(d2s("0"     ), MiscUtils.uszToStr(0L                  , true , true , 0));
        assertEquals(d2s("0"     ), MiscUtils.uszToStr(0L                  , true , false, 0));
        assertEquals(d2s("0"     ), MiscUtils.uszToStr(0L                  , false, true , 1));
        assertEquals(d2s("1"     ), MiscUtils.uszToStr(1L                  , true , true , 0));
        assertEquals(d2s("2"     ), MiscUtils.uszToStr(2L                  , true , false, 0));
        assertEquals(d2s("3"     ), MiscUtils.uszToStr(3L                  , false, true , 1));
        assertEquals(d2s("999"   ), MiscUtils.uszToStr(999L                , true , true , 0));
        assertEquals(d2s("999"   ), MiscUtils.uszToStr(999L                , true , false, 0));
        assertEquals(d2s("999"   ), MiscUtils.uszToStr(999L                , false, true , 1));
        assertEquals(d2s("1023"  ), MiscUtils.uszToStr(1023L               , false, true , 1));
        assertEquals(d2s("1k"    ), MiscUtils.uszToStr(1000L               , true , false, 0));
        assertEquals(d2s("1ki"   ), MiscUtils.uszToStr(1024L               , false, false, 0));
        assertEquals(d2s("1ki"   ), MiscUtils.uszToStr(1025L               , false, false, 0));
        assertEquals(d2s("1 ki"  ), MiscUtils.uszToStr(1025L               , false, true , 0));
        assertEquals(d2s("1 k"   ), MiscUtils.uszToStr(1000L               , true , true , 0));
        assertEquals(d2s("1.5k"  ), MiscUtils.uszToStr(1500L               , true , false, 1));
        assertEquals(d2s("1.1k"  ), MiscUtils.uszToStr(1099L               , true , false, 1));
        assertEquals(d2s("1.1k"  ), MiscUtils.uszToStr(1050L               , true , false, 1));
        assertEquals(d2s("1.0k"  ), MiscUtils.uszToStr(1049L               , true , false, 1));
        assertEquals(d2s("1.1k"  ), MiscUtils.uszToStr(1100L               , true , false, 1));
        assertEquals(d2s("1.9k"  ), MiscUtils.uszToStr(1949L               , true , false, 1));
        assertEquals(d2s("2.0k"  ), MiscUtils.uszToStr(1950L               , true , false, 1));
        assertEquals(d2s("2.22k" ), MiscUtils.uszToStr(2220L               , true , false, 2));
        assertEquals(d2s("2.22k" ), MiscUtils.uszToStr(2224L               , true , false, 2));
        assertEquals(d2s("1.50ki"), MiscUtils.uszToStr(1536L               , false, false, 2));
        assertEquals(d2s("1m"    ), MiscUtils.uszToStr(1000000L            , true , false, 0));
        assertEquals(d2s("1g"    ), MiscUtils.uszToStr(1000000000L         , true , false, 0));
        assertEquals(d2s("1t"    ), MiscUtils.uszToStr(1000000000000L      , true , false, 0));
        assertEquals(d2s("1p"    ), MiscUtils.uszToStr(1000000000000000L   , true , false, 0));
        assertEquals(d2s("1.5p"  ), MiscUtils.uszToStr(1500000000000000L   , true , false, 1));
        assertEquals(d2s("1.23p" ), MiscUtils.uszToStr(1230000000000000L   , true , false, 2));
        assertEquals(d2s("999p"  ), MiscUtils.uszToStr(999000000000000000L , true , false, 0));
    }

    @Test
    public void testCopyrightYear() {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1996);

        assertEquals("1996"     , MiscUtils.copyrightYear(1996, cal));
        assertEquals("1997"     , MiscUtils.copyrightYear(1997, cal));
        assertEquals("1995-1996", MiscUtils.copyrightYear(1995, cal));
        assertEquals("2010"     , MiscUtils.copyrightYear(2010, cal));
    }

    @Test
    public void testModifyLines() {
        final VarInt c = new VarInt();
        MiscUtils.LineModifier lm = rawLn -> {
            c.v++;
            return "x" + rawLn + "z";
        };
        assertTrue(""             .equals(MiscUtils.modifyLines(""         ,  1, lm))); assertTrue(c.v ==  0);
        assertTrue(""             .equals(MiscUtils.modifyLines(""         ,  0, lm))); assertTrue(c.v ==  0);
        assertTrue(""             .equals(MiscUtils.modifyLines(""         , -1, lm))); assertTrue(c.v ==  0);
        assertTrue("xyz"          .equals(MiscUtils.modifyLines("y"        ,  1, lm))); assertTrue(c.v ==  1);
        assertTrue("xyz"          .equals(MiscUtils.modifyLines("y"        , -1, lm))); assertTrue(c.v ==  2);
        assertTrue("xyz"          .equals(MiscUtils.modifyLines("y"        ,  2, lm))); assertTrue(c.v ==  3);
        assertTrue("xyz\nxYz"     .equals(MiscUtils.modifyLines("y\nY"     ,  2, lm))); assertTrue(c.v ==  5);
        assertTrue("xyz\nxYz"     .equals(MiscUtils.modifyLines("y\nY"     ,  3, lm))); assertTrue(c.v ==  7);
        assertTrue("xyz"          .equals(MiscUtils.modifyLines("y\nY"     ,  1, lm))); assertTrue(c.v ==  8);
        assertTrue("x1z\nx2z\nx3z".equals(MiscUtils.modifyLines("1\n2\n3\n", -1, lm))); assertTrue(c.v == 11);
    }

    @Test
    public void testMixedCase() {
        assertFalse(MiscUtils.mixedCase(""));
        assertFalse(MiscUtils.mixedCase("a"));
        assertFalse(MiscUtils.mixedCase("AB"));
        assertFalse(MiscUtils.mixedCase("ab123cb!e_!+_)@(#*$&%^{}|\\':';"));
        assertFalse(MiscUtils.mixedCase("A B C[OOOO]"));
        assertTrue (MiscUtils.mixedCase("aA"));
        assertTrue (MiscUtils.mixedCase("b___________-X"));
        assertTrue (MiscUtils.mixedCase("oooooooo#$*$^#&$!#A"));
    }

    @Test
    public void testLimitString() {
        assertEquals(MiscUtils.limitString(""          , 0, ""   ), "");
        assertEquals(MiscUtils.limitString("a"         , 0, "..."), "...");
        assertEquals(MiscUtils.limitString("abc"       , 0, "x"  ), "x");
        assertEquals(MiscUtils.limitString("abc"       , 1, "y"  ), "ay");
        assertEquals(MiscUtils.limitString("limit this", 9, "..."), "limit thi...");
    }

    @Test
    public void testPrintTime() {
        assertEquals(MiscUtils.printTime(0), "0s");
        assertEquals(MiscUtils.printTime(1), "1ms");
        assertEquals(MiscUtils.printTime(4), "4ms");
        assertEquals(MiscUtils.printTime(88), "88ms");
        assertEquals(MiscUtils.printTime(499), "499ms");
        assertEquals(MiscUtils.printTime(500), "0.5s");
        assertEquals(MiscUtils.printTime(549), "0.5s");
        assertEquals(MiscUtils.printTime(550), "0.6s");
        assertEquals(MiscUtils.printTime(999), "1.0s");
        assertEquals(MiscUtils.printTime(1049), "1.0s");
        assertEquals(MiscUtils.printTime(1050), "1.1s");
        assertEquals(MiscUtils.printTime(59949), "59.9s");
        assertEquals(MiscUtils.printTime(60049), "1m 0.0s");
        assertEquals(MiscUtils.printTime(60100), "1m 0.1s");
        assertEquals(MiscUtils.printTime(3600000), "1h 0m 0.0s");
        assertEquals(MiscUtils.printTime(7261749), "2h 1m 1.7s");
    }
}
