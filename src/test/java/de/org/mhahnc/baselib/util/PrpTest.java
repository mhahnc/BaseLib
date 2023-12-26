package de.org.mhahnc.baselib.util;

import static org.junit.Assert.assertTrue;

import java.util.Properties;
import org.junit.Test;

public class PrpTest {
    @Test
    public void testSelectByPrefix() {
        Properties props = new Properties();
        props.put("this.one.1", "1");
        props.put("this.one.2", "22");
        props.put("this.one.next.3", "333");
        props.put("something", "else");

        Properties p2 = Prp.selectByPrefix(props, "nil");
        assertTrue(0 == p2.size());
        p2 = Prp.selectByPrefix(props, "some");
        assertTrue(1 == p2.size());
        assertTrue("else".equals(p2.get("thing")));
        p2 = Prp.selectByPrefix(props, "this.one.");
        assertTrue(3 == p2.size());
        assertTrue("1".equals(p2.get("1")));
        assertTrue("22".equals(p2.get("2")));
        assertTrue("333".equals(p2.get("next.3")));
    }

    @Test
    public void testOverload() {
        Properties props = new Properties();

        String[] params = new String[] { };
        assertTrue(0 == Prp.overload(props, params));
        assertTrue(0 == props.size());
        params = new String[] { "x" };
        assertTrue(0 == Prp.overload(props, params));
        assertTrue(0 == props.size());
        params = new String[] { "x", "y" };
        assertTrue(0 == Prp.overload(props, params));
        assertTrue(0 == props.size());
        params = new String[] { "-x=1", "y" };
        assertTrue(1 == Prp.overload(props, params));
        assertTrue(1 == props.size());
        assertTrue("1".equals(props.getProperty("x")));
        params = new String[] { "-xyz=123", "something", "-x=1" };
        assertTrue(1 == Prp.overload(props, params));
        assertTrue(2 == props.size());
        assertTrue("123".equals(props.getProperty("xyz")));
        params = new String[] { "-xyz=777", "- x = 444\t"  };
        assertTrue(2 == Prp.overload(props, params));
        assertTrue(2 == props.size());
        assertTrue("777".equals(props.getProperty("xyz")));
        assertTrue(" 444\t".equals(props.getProperty("x")));
        params = new String[] { "-xyz=778", "-\tx\t=45", "end"  };
        assertTrue(2 == Prp.overload(props, params));
        assertTrue(2 == props.size());
        assertTrue("778".equals(props.getProperty("xyz")));
        assertTrue("45".equals(props.getProperty("x")));
        // now for the negative...
        props.clear();
        params = new String[] { "-x=1", "-=123" };
        assertTrue(-2 == Prp.overload(props, params));
        assertTrue(1 == props.size());
        assertTrue("1".equals(props.getProperty("x")));
        props.clear();
        params = new String[] { "-\t\t=", "123" };
        assertTrue(-1 == Prp.overload(props, params));
        assertTrue(0 == props.size());
        props.clear();
        params = new String[] { "-" };
        assertTrue(-1 == Prp.overload(props, params));
        assertTrue(0 == props.size());
    }
}
