package com.nfsdb.collections.experimental;

import java.util.Iterator;

public interface NWayHashMap<K, V> extends Iterable<NWayHashMap.Entry<K, V>> {

    void clear();

    V get(K key);

    Iterable<Entry<K, V>> immutableIterator();

    @Override
    Iterator<Entry<K, V>> iterator();

    K put(K key, V value);

    int size();

    class Entry<K, V> {
        public K key;
        public V value;
    }
}
