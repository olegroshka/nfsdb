package com.nfsdb.collections;

import com.nfsdb.collections.experimental.CopyBasedLRUNWayHashMap;
import com.nfsdb.collections.experimental.NWayHashMap;
import com.nfsdb.collections.experimental.RndNWayHashMap;
import com.nfsdb.collections.experimental.TimestampBasedLRUNWayHashMap;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;


public class NWayHashMapTest {

    private static int count = 1000000;

    private void test(NWayHashMap<String, Long> map) {
        map.put("k1", 1L);
        assertEquals(new Long(1L), map.get("k1"));

        for(long i=2; i <= count; i++) {
            String key = "k" + i;
            map.put(key, i);
        }
        for(long i=2; i <= count; i++) {
            String key = "k" + i;
            Long value = map.get(key);
            if(value != null) {
                assertEquals("failed: key: " + key, new Long(i), value);
            }
        }
    }

    @Test
    public void testBasicsCopyBasedLRU() {
        test(new CopyBasedLRUNWayHashMap(count));
    }

    @Test
    public void testBasicsTimestampBasedLRU() {
        test(new TimestampBasedLRUNWayHashMap(count));
    }

    @Test
    public void testBasicsRnd() {
        test(new RndNWayHashMap(count));
    }

}
