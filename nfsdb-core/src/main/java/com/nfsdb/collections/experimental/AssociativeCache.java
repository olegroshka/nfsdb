/*
 * Copyright (c) 2014. Oleg Roshka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nfsdb.collections.experimental;

import com.nfsdb.collections.AbstractImmutableIterator;
import com.nfsdb.utils.Numbers;
import com.nfsdb.utils.Unsafe;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Arrays;
import java.util.Iterator;


public class AssociativeCache<K, V> {

    public static class Entry<K, V> {
        public K key;
        public V value;
    }

    private static final int MIN_WAYS = 2;
    private static final int MIN_INITIAL_CAPACITY = 16;
    private final EntryIterator iterator = new EntryIterator();
    private final K[] keys;
    private final V[] values;
    private final int capacity;
    private final int mask;
    private final int wmask;
    private final int ways;
    private final int bits;

    public AssociativeCache(int initialCapacity) {
        this(8, initialCapacity);
    }

    @SuppressWarnings("unchecked")
    public AssociativeCache(int initialWays, int initialCapacity) {
        this.ways = Math.max(AssociativeCache.MIN_WAYS, Numbers.ceilPow2(initialWays - 1));
        this.capacity = Math.max(AssociativeCache.MIN_INITIAL_CAPACITY, Numbers.ceilPow2(initialCapacity - 1));
        int cells = capacity * this.ways;
        if( cells < 0) {
            throw new IllegalArgumentException("Overflow, reduce size or number of ways.");
        }
        this.keys = (K[]) new Object[cells];
        this.values = (V[]) new Object[cells];
        this.mask = capacity - 1;
        this.wmask = ways - 1;
        this.bits = 31 - Integer.numberOfLeadingZeros(ways);
        clear();
    }

    public V get(K key) {
        int firstCellIndex = (key.hashCode() & mask) << bits;
        int lastCellIndex = firstCellIndex + ways;
        for (int index = firstCellIndex; index < lastCellIndex; index++) {
            K k = Unsafe.arrayGet(keys, index);
            if (k == null) {
                return null;
            }

            if (k == key || key.equals(k)) {
                return Unsafe.arrayGet(values, index);
            }
        }
        return null;
    }

    public K put(K key, V value) {
        int firstCellIndex = (key.hashCode() & mask) << bits;
        int lastCellIndex = firstCellIndex + wmask;
        K oldKey = Unsafe.arrayGet(keys, lastCellIndex);
        int toIndex = firstCellIndex + 1;
        System.arraycopy(keys, firstCellIndex, keys, toIndex, wmask);
        System.arraycopy(values, firstCellIndex, values, toIndex, wmask);
        Unsafe.arrayPut(keys, firstCellIndex, key);
        Unsafe.arrayPut(values, firstCellIndex, value);

        return oldKey;
    }

    public final void clear() {
        Arrays.fill(keys, null);
    }

    public Iterator<Entry<K, V>> iterator() {
        iterator.index = 0;
        return iterator;
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

    public int size() {
        return capacity;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "capacity=" + capacity +
                ", ways=" + ways +
                '}';
    }

}
