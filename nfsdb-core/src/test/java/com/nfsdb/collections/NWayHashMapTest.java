package com.nfsdb.collections;

import com.nfsdb.collections.experimental.CopyBasedLRUNWayHashMap;
import com.nfsdb.collections.experimental.NWayHashMap;
import com.nfsdb.collections.experimental.RndNWayHashMap;
import com.nfsdb.collections.experimental.TimestampBasedLRUNWayHashMap;
import com.nfsdb.utils.Rnd;
import org.junit.Test;

import java.util.HashSet;

import static junit.framework.TestCase.assertEquals;


public class NWayHashMapTest {

    private static int count = 1000000;

    private void test(NWayHashMap<String, Long> map) {
        map.put("k1", 1L);
        assertEquals(new Long(1L), map.get("k1"));

        HashSet<String> evictedKeys = new HashSet<>();

        for(long i=2; i <= count; i++) {
            String key = "k" + i;
            String evictedKey = map.put(key, i);
            if( evictedKey != null ) {
                evictedKeys.add(evictedKey);
            }

        }
        for(long i=2; i <= count; i++) {
            String key = "k" + i;
            Long value = map.get(key);
            if(!evictedKeys.contains(key)) {
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
