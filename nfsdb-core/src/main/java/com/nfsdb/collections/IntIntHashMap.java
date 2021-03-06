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

package com.nfsdb.collections;

import com.nfsdb.utils.Numbers;
import com.nfsdb.utils.Unsafe;

import java.util.Arrays;


public class IntIntHashMap {

    public static final int MIN_INITIAL_CAPACITY = 16;
    private static final int noEntryValue = -1;
    private final double loadFactor;
    private int[] values;
    private int[] keys;
    private int free;
    private int mask;

    public IntIntHashMap() {
        this(8);
    }

    public IntIntHashMap(int initialCapacity) {
        this(initialCapacity, 0.5f);
    }

    @SuppressWarnings("unchecked")
    public IntIntHashMap(int initialCapacity, double loadFactor) {
        if (loadFactor <= 0d || loadFactor >= 1d) {
            throw new IllegalArgumentException("0 < loadFactor < 1");
        }
        int capacity = Math.max(initialCapacity, (int) (initialCapacity / loadFactor));
        capacity = capacity < MIN_INITIAL_CAPACITY ? MIN_INITIAL_CAPACITY : Numbers.ceilPow2(capacity);
        this.loadFactor = loadFactor;
        values = new int[capacity];
        keys = new int[capacity];
        free = initialCapacity;
        mask = capacity - 1;
        clear();
    }

    public final void clear() {
        Arrays.fill(values, noEntryValue);
    }

    public int get(int key) {
        int index = key & mask;
        if (Unsafe.arrayGet(values, index) == noEntryValue || Unsafe.arrayGet(keys, index) == key) {
            return Unsafe.arrayGet(values, index);
        }
        return probe(key, index);
    }

    public int put(int key, int value) {
        int old = insertKey(key, value);
        if (free == 0) {
            rehash();
        }
        return old;
    }

    private int insertKey(int key, int value) {
        int index = key & mask;
        if (Unsafe.arrayGet(values, index) == noEntryValue) {
            Unsafe.arrayPut(keys, index, key);
            Unsafe.arrayPut(values, index, value);
            free--;
            return noEntryValue;
        }

        if (Unsafe.arrayGet(keys, index) == key) {
            int r = Unsafe.arrayGet(values, index);
            Unsafe.arrayPut(values, index, value);
            return r;
        }

        return probeInsert(key, index, value);
    }

    private int probe(int key, int index) {
        do {
            index = (index + 1) & mask;
            if (Unsafe.arrayGet(values, index) == noEntryValue || Unsafe.arrayGet(keys, index) == key) {
                return Unsafe.arrayGet(values, index);
            }
        } while (true);
    }

    private int probeInsert(int key, int index, int value) {
        do {
            index = (index + 1) & mask;
            if (Unsafe.arrayGet(values, index) == noEntryValue) {
                Unsafe.arrayPut(keys, index, key);
                Unsafe.arrayPut(values, index, value);
                free--;
                return noEntryValue;
            }

            if (key == Unsafe.arrayGet(keys, index)) {
                int r = Unsafe.arrayGet(values, index);
                Unsafe.arrayPut(values, index, value);
                return r;
            }
        } while (true);
    }

    @SuppressWarnings({"unchecked"})
    protected void rehash() {

        int newCapacity = values.length << 1;
        mask = newCapacity - 1;

        free = (int) (newCapacity * loadFactor);

        int[] oldValues = values;
        int[] oldKeys = keys;
        this.keys = new int[newCapacity];
        this.values = new int[newCapacity];
        Arrays.fill(values, noEntryValue);

        for (int i = oldKeys.length; i-- > 0; ) {
            if (Unsafe.arrayGet(oldValues, i) != noEntryValue) {
                insertKey(Unsafe.arrayGet(oldKeys, i), Unsafe.arrayGet(oldValues, i));
            }
        }
    }
}
