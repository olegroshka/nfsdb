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


public class CopyBasedLRUNWayHashMap<K, V> extends NWayHashMapBase<K, V> {
    public CopyBasedLRUNWayHashMap() {
        this(128);
    }

    public CopyBasedLRUNWayHashMap(int initialCapacity) {
        this(8, initialCapacity);
    }

    @SuppressWarnings("unchecked")
    public CopyBasedLRUNWayHashMap(int initialWays, int initialCapacity) {
        super(initialWays, initialCapacity);
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

}
