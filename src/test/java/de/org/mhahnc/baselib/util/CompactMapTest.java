package de.org.mhahnc.baselib.util;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

public class CompactMapTest {
    @Test
    public void test0() {
        CompactMap<String,Integer> cm = new CompactMap<>();
        assertNull(cm.get("x"));
        assertNull(cm.get(null));
        cm.put("y", 123);
        cm.put("zzz", 456789);
        assertNull(cm.get("x"));
        assertTrue(123    == cm.get(null));
        assertTrue(123    == cm.get("y"));
        assertTrue(456789 == cm.get("zzz"));
        assertTrue(null   == cm.get("zz"));
        for (int i = 0; i < 10000000; i++) {
            cm.put("y", i);
            assertTrue(i == cm.get("y"));
        }
    }
}
