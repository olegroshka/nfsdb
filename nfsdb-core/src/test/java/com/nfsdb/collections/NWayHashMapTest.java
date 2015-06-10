package com.nfsdb.collections;

import com.nfsdb.collections.experimental.CopyBasedLRUNWayHashMap;
import com.nfsdb.collections.experimental.NWayHashMap;
import com.nfsdb.collections.experimental.RndNWayHashMap;
import com.nfsdb.collections.experimental.TimestampBasedLRUNWayHashMap;
import com.nfsdb.utils.Rnd;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
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


    private static class Key {
        int hash;
        String text;

        public Key(int hash, String text) {
            this.hash = hash;
            this.text = text;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    @Test
    public void tesLRUEvictionOnPut() {
        CopyBasedLRUNWayHashMap<Key, String> map = new CopyBasedLRUNWayHashMap(4, 8);
        Key key1 = new Key(1, "key1");
        assertEquals(null, map.put(key1, "v1"));
        Key key2 = new Key(1, "key2");
        assertEquals(null, map.put(key2, "v2"));
        Key key3 = new Key(1, "key3");
        assertEquals(null, map.put(key3, "v3"));
        Key key4 = new Key(1, "key4");
        assertEquals(null, map.put(key4, "v4"));
        Key key5 = new Key(1, "key5");
        assertEquals(key1, map.put(key5, "v5"));
    }


}
