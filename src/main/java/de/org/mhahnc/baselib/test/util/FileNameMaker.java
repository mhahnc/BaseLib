package de.org.mhahnc.baselib.test.util;

import java.util.ArrayList;
import java.util.List;

import de.org.mhahnc.baselib.util.MiscUtils;

public abstract class FileNameMaker {
    public abstract String make(int nbytes);

    final static char[] RESERVED_CHARS = new char[] {
        // TODO: add more if proven to be reserved in any FS (or Unicode)
        '/', '\\', ':', '?', '<', '>', '*', '|', '"', '~', 0xfeff
    };
    public final static boolean isResvChar(char c, boolean firstLast) {
        if (' ' > c) {
            return true;
        }
        if (' ' == c && firstLast) {
            return true;
        }
        for (char r : RESERVED_CHARS) {
            if (r == c) {
                return true;
            }
        }
        return false;
    }
    public static class Numbered extends FileNameMaker {
        long number;
        public String make(final int nbytes) {
            String result = String.format("%d", this.number++);

            int pad = nbytes - result.length();
            if (pad >= 0) {
                return MiscUtils.fillString(pad, '0') + result;
            }
            return result.substring(-pad);
        }
    }
    public abstract static class Random extends FileNameMaker {
        protected abstract int randomBase();
        protected java.util.Random rnd = new java.util.Random(randomBase());
    }
    public static class RandomASCII extends Random {
        protected int randomBase() { return 0xf1137a73; }
        public String make(final int nbytes) {
            final java.util.Random r = this.rnd;
            final char[] result = new char[nbytes];
            for (int pos = 0, l = result.length; pos < l;) {
                char c = (char)(r.nextInt(127 - ' ') + ' ');
                if (isResvChar(c, 0 == pos || pos == l - 1)) {
                    continue;
                }
                result[pos++] = c;
            }
            return new String(result);
        }
    }
    public static class RandomDE extends Random {
        protected int randomBase() { return 0x19451990; }
        Character[] tab;
        public RandomDE() {
            List<Character> clst = new ArrayList<>();
            for (char c = ' '; c < (char)128; c++) {
                if (isResvChar(c, c == ' ' || c == (char)127)) {
                    continue;
                }
                clst.add(c);
            }
            clst.add('\u00E4'); // ae
            clst.add('\u00C4'); // AE
            clst.add('\u00F6'); // oe
            clst.add('\u00D6'); // OE
            clst.add('\u00FC'); // ue
            clst.add('\u00DC'); // UE
            clst.add('\u00DF'); // ss
            this.tab = clst.toArray(new Character[clst.size()]);
        }
        public String make(final int nbytes) {
            final char[] result = new char[nbytes];
            for (int pos = 0; pos < result.length; pos++) {
                result[pos] = this.tab[this.rnd.nextInt(this.tab.length)];
            }
            return new String(result);
        }
    }
    public static class RandomUnicode extends Random {
        protected int randomBase() { return 0x12345678; }
        public String make(final int nbytes) {
            int l = nbytes >> 1;
            final char[] result = new char[l];
            for (int pos = 0; pos < l;) {
                final java.util.Random r = this.rnd;
                // excludes some reserved Unicode range automatically
                // FIXME: Java 7 u51 under OSX suddenly has some serious issues
                //        dealing with Unicode file names (or at least certain
                //        codes), or OSX 10.9 cannot deal with such names either
                //        (was able to create file names not removable in the
                //        terminal, odd) - hence we go conservative for now ...
                int maxCode = MiscUtils.underMacOS() ? 256 : 65536;
                char c = (char)(r.nextInt(maxCode - 32 - 16) + 32);
                if (isResvChar(c, 0 == pos || pos == l - 1)) {
                    continue;
                }
                result[pos++] = c;
            }
            return new String(result);
        }
    }
    public static class Mixer extends Random {
        protected int randomBase() { return 0xc001bebe; }
        public Mixer(FileNameMaker[] makers) {
            this.makers = makers;
        }
        FileNameMaker[] makers;
        public String make(final int nbytes) {
            return this.makers[this.rnd.nextInt(
                   this.makers.length)].make(nbytes);
        }
    }
    public abstract static class Filtered extends FileNameMaker {
        public Filtered(FileNameMaker fnmk) {
            this.fnmk = fnmk;
        }
        FileNameMaker fnmk;
        public String make(int nbytes) {
            for(;;) {
                String result = this.fnmk.make(nbytes);
                if (filter(result)) {
                    continue;
                }
                return result;
            }
        }
        protected abstract boolean filter(String name);
    }
}

