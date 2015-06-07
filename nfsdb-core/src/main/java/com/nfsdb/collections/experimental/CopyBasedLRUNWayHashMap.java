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
        int firstCellIndex = (key.hashCode() & mask) * ways;
        for (int index = firstCellIndex; index < firstCellIndex + ways; index++) {
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
        K oldKey = null;
        int firstCellIndex = (key.hashCode() & mask) * ways;
        for (int index = firstCellIndex; index < firstCellIndex + ways; index++) {
            oldKey = Unsafe.arrayGet(keys, index);
            if (oldKey == null) {
                Unsafe.arrayPut(keys, index, key);
                Unsafe.arrayPut(values, index, value);
                return null;
            }

            if (oldKey == key || key.equals(oldKey)) {
                System.arraycopy(keys, firstCellIndex, keys, firstCellIndex + 1, index - firstCellIndex);
                System.arraycopy(values, firstCellIndex, values, firstCellIndex + 1, index - firstCellIndex);
                Unsafe.arrayPut(keys, firstCellIndex, key);
                Unsafe.arrayPut(values, firstCellIndex, value);
                return oldKey;
            }
        }
        //no slots available, shift down and insert in the first cell
        System.arraycopy(keys, firstCellIndex, keys, firstCellIndex + 1, ways - 1);
        System.arraycopy(values, firstCellIndex, values, firstCellIndex + 1, ways - 1);
        Unsafe.arrayPut(keys, firstCellIndex, key);
        Unsafe.arrayPut(values, firstCellIndex, value);

        return oldKey;
    }

}
