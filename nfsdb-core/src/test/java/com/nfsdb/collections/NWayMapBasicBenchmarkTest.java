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

    private int iterations;
    private int ways;
    private int size;

    public NWayMapBasicBenchmarkTest(int iterations, int ways, int size) {
        this.iterations = iterations;
        this.ways = ways;
        this.size = size;
    }

    @Parameterized.Parameters
    public static Collection scenarios() {
        return Arrays.asList(
                new Object[]{10000,   4, 1000},
                new Object[]{100000,  4, 10000},
                new Object[]{1000000, 4, 100000},
                new Object[]{10000000,4, 100000},
                new Object[]{10000000,4, 10000000},
                new Object[]{10000,   8, 1000},
                new Object[]{100000,  8, 10000},
                new Object[]{1000000, 8, 100000},
                new Object[]{10000000,8, 100000},
                new Object[]{10000000,8, 10000000},
                new Object[]{10000,   16, 1000},
                new Object[]{100000,  16, 10000},
                new Object[]{1000000, 16, 100000},
                new Object[]{10000000,16, 100000},
                new Object[]{10000000,16, 10000000},
                new Object[]{10000,   64, 1000},
                new Object[]{100000,  64, 10000},
                new Object[]{1000000, 64, 100000},
                new Object[]{10000000,64, 100000},
                new Object[]{10000000,64, 10000000}
        );

    }

    private static List<String> results = new ArrayList<>();

    private void runScenario(NWayHashMap<Integer,Integer> map) {
        runTest(map);
        long start = System.nanoTime();
        runTest(map);
        results.add(map.getClass().getSimpleName() + ", " + ways + ", " + map.size() + ", " + iterations + ", " + (System.nanoTime() - start));
    }

    private void runTest(NWayHashMap<Integer, Integer> map) {
        Rnd rnd = new Rnd(System.currentTimeMillis(), System.nanoTime());
        for (int i = 0; i < iterations; i++) {
            int key = rnd.nextInt();
            if( map.get(key) == null ) {
                map.put(key, rnd.nextInt());
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
