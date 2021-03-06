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

import com.nfsdb.collections.AbstractImmutableIterator;
import com.nfsdb.utils.Interval;

public class MillisIntervalSource extends AbstractImmutableIterator<Interval> implements IntervalSource {
    private final Interval start;
    private final Interval next;
    private final long period;
    private final int count;
    private int pos = 0;

    public MillisIntervalSource(Interval start, long period, int count) {
        this.start = start;
        this.period = period;
        this.count = count;
        this.next = new Interval(start.getLo(), start.getHi());
    }

    @Override
    public boolean hasNext() {
        return pos < count;
    }

    @Override
    public Interval next() {
        if (pos++ == 0) {
            return start;
        } else {
            next.update(next.getLo() + period, next.getHi() + period);
            return next;
        }
    }

    @Override
    public void reset() {
        pos = 0;
        next.update(start.getLo(), start.getHi());
    }
}