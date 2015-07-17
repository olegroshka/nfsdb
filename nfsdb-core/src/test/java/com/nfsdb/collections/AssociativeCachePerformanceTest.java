package com.nfsdb.collections;

import com.nfsdb.utils.Rnd;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Ignore
@RunWith(Parameterized.class)
public class AssociativeCachePerformanceTest {

    private static String SYMBOLS = "1234567890qwertyuioasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";
    private int iterations;
    private int ways;
    private int size;
    private CharSequence[] keys;
    private Rnd rnd = new Rnd();

    public AssociativeCachePerformanceTest(int iterations, int ways, int size) {
        this.iterations = iterations;
        this.ways = ways;
        this.size = size;

        this.keys = new CharSequence[size];
        for (int i = 0; i < size; i++) {
            int keySize = rnd.nextInt() & 77;
            char[] keyChars = new char[keySize];
            int m = SYMBOLS.length() - 1;
            for (int j = 0; j < keySize; j++) {
                keyChars[j] = SYMBOLS.charAt(rnd.nextInt() & m);
            }
            keys[i] = new String(keyChars);
        }
    }

    @Parameterized.Parameters
    public static Collection scenarios() {
        return Arrays.asList(
                new Object[]{15000,   4, 5000},
                new Object[]{100000,  4, 100000},
                new Object[]{1000000, 4, 1000000},
                new Object[]{10000000,4, 10000000},
                new Object[]{15000,   8, 5000},
                new Object[]{100000,  8, 100000},
                new Object[]{1000000, 8, 1000000},
                new Object[]{10000000,8, 10000000},
                new Object[]{15000,   16, 5000},
                new Object[]{100000,  16, 100000},
                new Object[]{1000000, 16, 1000000},
                new Object[]{10000000,16, 10000000},
                new Object[]{15000,   32, 5000},
                new Object[]{100000,  32, 100000},
                new Object[]{1000000, 32, 1000000},
                new Object[]{10000000,32, 10000000},
                new Object[]{15000,   64, 5000},
                new Object[]{100000,  64, 100000},
                new Object[]{1000000, 64, 1000000},
                new Object[]{10000000,64, 10000000}
        );

    }

    private static List<String> results = new ArrayList<>();

    private void runScenario(AssociativeCache<CharSequence, CharSequence> cache) {
        int m = size - 1;
        long start = 0;
        for (int i = -iterations; i < iterations; i++) {
            CharSequence key = keys[i & m];
            if( cache.get(key) == null ) {
                cache.put(key, key);
            }
            if(i == 0) {
                start = System.nanoTime();
            }
        }
        results.add(cache.getClass().getSimpleName() + ", " + ways + ", " + cache.capacity() + ", " + iterations + ", " + (System.nanoTime() - start));
    }

    @Test
    public void testCopyBasedLRUImplementation() {
        runScenario(new AssociativeCache<>(ways, size));
    }

    @AfterClass
    public static void printResults() {
        results.forEach(r -> System.out.println(r) );
    }
}
