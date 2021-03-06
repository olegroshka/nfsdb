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

package com.nfsdb.ql.impl;

import com.nfsdb.collections.ObjIntHashMap;
import com.nfsdb.collections.ObjList;
import com.nfsdb.exceptions.JournalRuntimeException;
import com.nfsdb.factory.configuration.RecordColumnMetadata;
import com.nfsdb.ql.RecordMetadata;

import java.util.Arrays;

public class SelectedColumnsMetadata implements RecordMetadata {
    private final RecordMetadata delegate;
    private final int reindex[];
    private final ObjIntHashMap<CharSequence> nameIndex;

    public SelectedColumnsMetadata(RecordMetadata delegate, ObjList<String> names) {
        this.delegate = delegate;
        int k = names.size();
        this.nameIndex = new ObjIntHashMap<>(k);
        this.reindex = new int[k];
        for (int i = 0; i < k; i++) {
            reindex[i] = delegate.getColumnIndex(names.getQuick(i));
            nameIndex.put(names.getQuick(i), i);
        }
    }

    @Override
    public RecordColumnMetadata getColumn(int index) {
        return delegate.getColumn(reindex[index]);
    }

    @Override
    public RecordColumnMetadata getColumn(CharSequence name) {
        return getColumn(getColumnIndex(name));
    }

    @Override
    public int getColumnCount() {
        return reindex.length;
    }

    @Override
    public int getColumnIndex(CharSequence name) {
        int index = nameIndex.get(name);
        if (index == -1) {
            throw new JournalRuntimeException("Invalid column name %s", name);
        }
        return index;
    }

    @Override
    public boolean invalidColumn(CharSequence name) {
        return nameIndex.get(name) == -1;
    }

    @Override
    public String toString() {
        return "SelectedColumnsMetadata{" +
                "delegate=" + delegate +
                ", reindex=" + Arrays.toString(reindex) +
                '}';
    }
}
