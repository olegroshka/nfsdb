package com.nfsdb.collections;

import com.nfsdb.collections.experimental.CopyBasedLRUNWayHashMap;
import com.nfsdb.collections.experimental.NWayHashMap;
import com.nfsdb.collections.experimental.RndNWayHashMap;
import com.nfsdb.collections.experimental.TimestampBasedLRUNWayHashMap;
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
public class NWayMapBasicBenchmarkTest {

    private static String SYMBOLS = "1234567890qwertyuioasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";
    private int iterations;
    private int ways;
    private int size;
    private CharSequence[] keys;
    private Rnd rnd = new Rnd(System.currentTimeMillis(), System.nanoTime());

    public NWayMapBasicBenchmarkTest(int iterations, int ways, int size) {
        this.iterations = iterations;
        this.ways = ways;
        this.size = size;

        this.keys = new CharSequence[iterations];
        for (int i = 0; i < iterations; i++) {
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
//                new Object[]{10000000,4, 10000000},
//                new Object[]{10000000,4, 5000000},
//                new Object[]{10000,   8, 1000},
//                new Object[]{100000,  8, 10000},
//                new Object[]{1000000, 8, 100000},
//                new Object[]{10000000,8, 100000},
//                new Object[]{10000000,8, 5000000}
                new Object[]{15000,   16, 5000},
                new Object[]{100000,  16, 100000},
                new Object[]{1000000, 16, 1000000},
//                new Object[]{10000000,16, 100000},
//                new Object[]{10000000,16, 5000000},
                new Object[]{15000,   64, 5000},
                new Object[]{100000,  64, 100000},
                new Object[]{1000000, 64, 1000000}
//                new Object[]{10000000,64, 100000},
//                new Object[]{10000000,64, 5000000}
        );

    }

    private static List<String> results = new ArrayList<>();

    private void runScenario(NWayHashMap<CharSequence, CharSequence> map) {
        warm(map);

        for (int i = 0; i < iterations; i++) {
            if( rnd.nextBoolean() ) {
                CharSequence key = keys[i];
                map.put(key, key);
            }
        }
        long start = System.nanoTime();
        runTest(map);
        results.add(map.getClass().getSimpleName() + ", " + ways + ", " + map.size() + ", " + iterations + ", " + (System.nanoTime() - start));
    }

    private void warm(NWayHashMap<CharSequence, CharSequence> map) {
        int count = Math.min(15000, iterations);
        for (int i = 0; i < count; i++) {
            CharSequence key = keys[i];
            map.get(key);
            map.put(key, key);
        }
        map.clear();
    }

    private void runTest(NWayHashMap<CharSequence, CharSequence> map) {
        for (int i = 0; i < iterations; i++) {
            CharSequence key = keys[i];
            if( map.get(key) == null ) {
                map.put(key, key);
            }
        }
    }

    @Test
    public void testTimestampBasedLRUImplementation() {
        runScenario(new TimestampBasedLRUNWayHashMap<>(ways, size));
    }

    @Test
    public void testCopyBasedLRUImplementation() {
        runScenario(new CopyBasedLRUNWayHashMap<>(ways, size));
    }

    @Test
    public void testRandomBasedImplementation() {
        runScenario(new RndNWayHashMap<>(ways, size));
    }

    @AfterClass
    public static void printResults() {
        results.forEach(r -> System.out.println(r) );
    }
}
