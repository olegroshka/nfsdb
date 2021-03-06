/*
 * Copyright (c) 2014-2015. Vlad Ilyushchenko
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

import com.nfsdb.collections.DirectInputStream;
import com.nfsdb.collections.ObjList;
import com.nfsdb.io.sink.CharSink;
import com.nfsdb.ql.Record;
import com.nfsdb.ql.RecordMetadata;
import com.nfsdb.ql.ops.VirtualColumn;

import java.io.OutputStream;

public class VirtualRecord extends AbstractRecord {
    private final int split;
    private final ObjList<VirtualColumn> virtualColumns;
    private Record base;

    public VirtualRecord(RecordMetadata metadata, int split, ObjList<VirtualColumn> virtualColumns) {
        super(metadata);
        this.split = split;
        this.virtualColumns = virtualColumns;
    }

    @Override
    public byte get(int col) {
        return col < split ? base.get(col) : virtualColumns.get(col - split).get();
    }

    @Override
    public void getBin(int col, OutputStream s) {
        if (col < split) {
            base.getBin(col, s);
        } else {
            virtualColumns.get(col - split).getBin(s);
        }
    }

    @Override
    public DirectInputStream getBin(int col) {
        return col < split ? base.getBin(col) : virtualColumns.get(col - split).getBin();
    }

    @Override
    public boolean getBool(int col) {
        return col < split ? base.getBool(col) : virtualColumns.get(col - split).getBool();
    }

    @Override
    public long getDate(int col) {
        return col < split ? base.getDate(col) : virtualColumns.get(col - split).getDate();
    }

    @Override
    public double getDouble(int col) {
        return col < split ? base.getDouble(col) : virtualColumns.get(col - split).getDouble();
    }

    @Override
    public float getFloat(int col) {
        return col < split ? base.getFloat(col) : virtualColumns.get(col - split).getFloat();
    }

    @Override
    public CharSequence getFlyweightStr(int col) {
        return col < split ? base.getFlyweightStr(col) : virtualColumns.get(col - split).getFlyweightStr();
    }

    @Override
    public int getInt(int col) {
        return col < split ? base.getInt(col) : virtualColumns.get(col - split).getInt();
    }

    @Override
    public long getLong(int col) {
        return col < split ? base.getLong(col) : virtualColumns.get(col - split).getLong();
    }

    @Override
    public long getRowId() {
        return 0;
    }

    @Override
    public short getShort(int col) {
        return col < split ? base.getShort(col) : virtualColumns.get(col - split).getShort();
    }

    @Override
    public CharSequence getStr(int col) {
        return col < split ? base.getStr(col) : virtualColumns.get(col - split).getStr();
    }

    @Override
    public void getStr(int col, CharSink sink) {
        if (col < split) {
            base.getStr(col, sink);
        } else {
            virtualColumns.get(col - split).getStr(sink);
        }
    }

    @Override
    public String getSym(int col) {
        return col < split ? base.getSym(col) : virtualColumns.get(col - split).getSym();
    }

    public void setBase(Record base) {
        this.base = base;
    }
}
