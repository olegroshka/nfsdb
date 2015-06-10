package com.nfsdb.collections.experimental;

import com.nfsdb.collections.AbstractImmutableIterator;
import com.nfsdb.utils.Numbers;
import com.nfsdb.utils.Unsafe;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by olegroshka on 07/06/2015.
 */
public abstract class NWayHashMapBase<K, V> implements NWayHashMap<K, V> {
    public static final int MIN_WAYS = 4;
    public static final int MIN_INITIAL_CAPACITY = 16;
    private final EntryIterator iterator = new EntryIterator();
    protected K[] keys;
    protected V[] values;
    protected int capacity;
    protected int mask;
    protected int ways;
    protected int wmask;
    protected int bits;

    public NWayHashMapBase(int initialWays, int initialCapacity) {
        this.ways = Numbers.ceilPow2(Math.max(MIN_WAYS, initialWays - 1));
        int capacity = Math.max(initialCapacity, initialCapacity);
        capacity = capacity < MIN_INITIAL_CAPACITY ? MIN_INITIAL_CAPACITY : Numbers.ceilPow2(capacity);
        this.capacity = capacity;
        int cells = capacity * this.ways;
        this.keys = (K[]) new Object[cells];
        this.values = (V[]) new Object[cells];
        this.mask = capacity - 1;
        this.wmask = ways - 1;
        this.bits = 31 - Integer.numberOfLeadingZeros(ways);
        clear();
    }


    @Override
    public final void clear() {
        Arrays.fill(keys, null);
    }

    @Override
    public Iterable<Entry<K, V>> immutableIterator() {
        return new EntryIterator();
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
        iterator.index = 0;
        return iterator;
    }

    @Override
    public int size() {
        return capacity;
    }

    public class EntryIterator extends AbstractImmutableIterator<Entry<K, V>> {

        private final Entry<K, V> entry = new Entry<>();
        private int index = 0;

        @Override
        public boolean hasNext() {
            return index < values.length && (Unsafe.arrayGet(keys, index) != null || scan());
        }

        @SuppressFBWarnings({"IT_NO_SUCH_ELEMENT"})
        @Override
        public Entry<K, V> next() {
            entry.key = keys[index];
            entry.value = values[index++];
            return entry;
        }

        private boolean scan() {
            do {
                index++;
            } while (index < values.length && Unsafe.arrayGet(keys, index) == null);
            return index < values.length;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "capacity=" + capacity +
                ", ways=" + ways +
                '}';
    }

}
