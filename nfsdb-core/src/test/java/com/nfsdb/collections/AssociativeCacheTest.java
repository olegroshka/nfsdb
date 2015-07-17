package com.nfsdb.collections;

import junit.framework.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertEquals;


public class AssociativeCacheTest {

    private static int count = 1000000;

    private void test(AssociativeCache<String, Long> cache) {
        cache.put("k1", 1L);
        assertEquals(new Long(1L), cache.get("k1"));

        HashSet<String> evictedKeys = new HashSet<>();

        for(long i=2; i <= count; i++) {
            String key = "k" + i;
            String evictedKey = cache.put(key, i);
            if( evictedKey != null ) {
                evictedKeys.add(evictedKey);
            }

        }
        for(long i=2; i <= count; i++) {
            String key = "k" + i;
            Long value = cache.get(key);
            if(!evictedKeys.contains(key)) {
                assertEquals("failed: key: " + key, new Long(i), value);
            }
        }
    }

    @Test
    public void testBasicsCopyBasedLRU() {
        test(new AssociativeCache(count));
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
        AssociativeCache<Key, String> cache = new AssociativeCache(4, 16);
        Key key1 = new Key(1, "key1");
        assertNull(cache.get(key1));
        assertNull(cache.put(key1, "v1"));
        Key key2 = new Key(1, "key2");
        assertNull(cache.get(key2));
        assertNull(cache.put(key2, "v2"));
        Key key3 = new Key(1, "key3");
        assertNull(cache.put(key3, "v3"));
        Key key4 = new Key(1, "key4");
        assertNull(cache.put(key4, "v4"));
        Key key5 = new Key(1, "key5");
        assertEquals(key1, cache.put(key5, "v5"));

        Assert.assertEquals(16, cache.capacity());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOverflowException() {
        new AssociativeCache<String, String>(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Test
    public void testIterator() {
        int size = 1000;
        AssociativeCache<Integer, Integer> cache = new AssociativeCache<>(16, size);
        Map<Integer, Integer> refMap = new HashMap<>(size);
        new Random().ints().limit(size).forEach(i -> {
            int v = i * i;
            refMap.put(i, v);
            cache.put(i, v);
        });
        cache.iterator().forEachRemaining(e -> assertEquals(refMap.get(e.key), cache.get(e.key)));
    }

    public void permutation(String[] words, int w, int p) {
        for (String word : words) {
            for (int i = 0; i < word.length(); i++) {
                System.out.print(word.charAt(i));
            }
        }
    }

}
