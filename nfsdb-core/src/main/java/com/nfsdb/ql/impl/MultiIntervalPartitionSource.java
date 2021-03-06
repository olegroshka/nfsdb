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

import com.nfsdb.collections.AbstractImmutableIterator;
import com.nfsdb.exceptions.JournalException;
import com.nfsdb.factory.JournalReaderFactory;
import com.nfsdb.factory.configuration.JournalMetadata;
import com.nfsdb.ql.PartitionCursor;
import com.nfsdb.ql.PartitionSlice;
import com.nfsdb.ql.PartitionSource;
import com.nfsdb.storage.BSearchType;
import com.nfsdb.storage.FixedColumn;
import com.nfsdb.utils.Interval;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class MultiIntervalPartitionSource extends AbstractImmutableIterator<PartitionSlice> implements PartitionSource, PartitionCursor {
    private final PartitionSource partitionSource;
    private final PartitionSlice result = new PartitionSlice();
    private final IntervalSource intervalSource;
    private PartitionCursor partitionCursor;
    private boolean needInterval = true;
    private boolean needPartition = true;
    private Interval interval;
    private PartitionSlice slice = null;
    private FixedColumn timestampColumn = null;
    private long nextRowLo;

    public MultiIntervalPartitionSource(PartitionSource partitionSource, IntervalSource intervalSource) {
        this.partitionSource = partitionSource;
        this.intervalSource = intervalSource;
    }

    @Override
    public JournalMetadata getMetadata() {
        return partitionSource.getMetadata();
    }

    @Override
    public PartitionCursor prepareCursor(JournalReaderFactory readerFactory) throws JournalException {
        partitionCursor = partitionSource.prepareCursor(readerFactory);
        return this;
    }

    @Override
    public boolean hasNext() {
        long sliceRowLo;
        long sliceRowHi;

        while (true) {

            if (needInterval) {
                if (intervalSource.hasNext()) {
                    interval = intervalSource.next();
                } else {
                    return false;
                }
            }

            if (needPartition) {
                if (partitionCursor.hasNext()) {
                    slice = partitionCursor.next();
                    sliceRowLo = nextRowLo = slice.lo;
                    sliceRowHi = slice.calcHi ? slice.partition.size() - 1 : slice.hi;
                    if (sliceRowHi < 0) {
                        needInterval = false;
                        continue;
                    }
                    timestampColumn = slice.partition.getTimestampColumn();
                } else {
                    return false;
                }
            } else {
                sliceRowLo = nextRowLo;
                sliceRowHi = slice.calcHi ? slice.partition.size() - 1 : slice.hi;
            }

            long sliceLo;
            long sliceHi;

            // interval is fully above notional partition interval, skip to next interval
            if (interval.getHi() < slice.partition.getInterval().getLo() || interval.getHi() < (sliceLo = timestampColumn.getLong(sliceRowLo))) {
                needPartition = false;
                needInterval = true;
                continue;
            }

            // interval is below notional partition, skip to next partition
            if (interval.getLo() > slice.partition.getInterval().getHi() || interval.getLo() > (sliceHi = timestampColumn.getLong(sliceRowHi))) {
                needPartition = true;
                needInterval = false;
                continue;
            }

            this.result.partition = slice.partition;

            if (interval.getLo() > sliceLo) {
                this.result.lo = slice.partition.indexOf(interval.getLo(), BSearchType.NEWER_OR_SAME);
            } else {
                this.result.lo = sliceRowLo;
            }

            if (interval.getHi() < sliceHi) {
                this.result.hi = slice.partition.indexOf(interval.getHi(), BSearchType.OLDER_OR_SAME, this.result.lo, sliceRowHi);
                needPartition = false;
                needInterval = true;
            } else {
                this.result.hi = sliceRowHi;
                needPartition = true;
                needInterval = interval.getHi() == sliceHi;
            }

            nextRowLo = result.hi + 1;

            return true;
        }
    }

    @SuppressFBWarnings({"IT_NO_SUCH_ELEMENT"})
    @Override
    public PartitionSlice next() {
        return result;
    }

    @Override
    public void reset() {
        intervalSource.reset();
        needInterval = true;
        needPartition = true;
    }
}
