/*
 * Copyright (c) 2014. Vlad Ilyushchenko
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

import com.nfsdb.utils.Unsafe;


public class TimestampBasedLRUNWayHashMap<K, V> extends NWayHashMapBase<K, V> {

    private long[] timestamps;

    public TimestampBasedLRUNWayHashMap() {
        this(128);
    }

    public TimestampBasedLRUNWayHashMap(int initialCapacity) {
        this(8, initialCapacity);
    }

    @SuppressWarnings("unchecked")
    public TimestampBasedLRUNWayHashMap(int initialWays, int initialCapacity) {
        super(initialWays, initialCapacity);
        this.timestamps = new long[capacity * ways];
    }

    public V get(K key) {
        int firstCellIndex = (key.hashCode() & mask) << bits;
        int lastCellIndex = firstCellIndex + ways;
        for (int index = firstCellIndex; index < lastCellIndex; index++) {
            K k = Unsafe.arrayGet(keys, index);
            if (k == key || key.equals(k)) {
                Unsafe.arrayPut(timestamps, index, System.nanoTime());
                return Unsafe.arrayGet(values, index);
            }
        }
        return null;
    }

    public K put(K key, V value) {
        K oldKey;
        int firstCellIndex = (key.hashCode() & mask) << bits;
        int lastCellIndex = firstCellIndex + ways;
        long lruTimestamp = Long.MAX_VALUE;
        int lruIndex = firstCellIndex;
        for (int index = firstCellIndex; index < lastCellIndex; index++) {
            oldKey = Unsafe.arrayGet(keys, index);
            if (oldKey == null) {
                Unsafe.arrayPut(keys, index, key);
                Unsafe.arrayPut(values, index, value);
                Unsafe.arrayPut(timestamps, index, System.nanoTime());
                return null;
            }
            long timestamp = Unsafe.arrayGet(timestamps, index);
            if( timestamp < lruTimestamp ) {
                lruTimestamp = timestamp;
                lruIndex = index;
            }
        }
        //no slots available, evicting lru element
        oldKey = Unsafe.arrayGet(keys, lruIndex);
        Unsafe.arrayPut(keys, lruIndex, key);
        Unsafe.arrayPut(values, lruIndex, value);
        Unsafe.arrayPut(timestamps, lruIndex, System.nanoTime());

        return oldKey;
    }

}
