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

package com.nfsdb.ql;

import com.nfsdb.JournalWriter;
import com.nfsdb.exceptions.JournalException;
import com.nfsdb.model.Quote;
import com.nfsdb.ql.impl.JournalPartitionSource;
import com.nfsdb.ql.impl.JournalSource;
import com.nfsdb.ql.impl.KvIndexLookupRowSource;
import com.nfsdb.ql.impl.MergingRowSource;
import com.nfsdb.ql.ops.StringConstant;
import com.nfsdb.test.tools.AbstractTest;
import com.nfsdb.test.tools.TestUtils;
import com.nfsdb.utils.Dates;
import org.junit.Assert;
import org.junit.Test;

public class MergingRowSourceTest extends AbstractTest {
    @Test
    public void testMerge() throws JournalException {
        JournalWriter<Quote> w = factory.writer(Quote.class);
        TestUtils.generateQuoteData(w, 100000, Dates.parseDateTime("2014-02-11T00:00:00.000Z"), 10);

        RowSource srcA = new KvIndexLookupRowSource("sym", new StringConstant("BP.L"), true);
        RowSource srcB = new KvIndexLookupRowSource("sym", new StringConstant("WTB.L"), true);

        RecordSource<? extends Record> rs = new JournalSource(new JournalPartitionSource(w, true), new MergingRowSource(srcA, srcB));

        long last = 0;
        RecordCursor<? extends Record> c = rs.prepareCursor(null);
        int ts = rs.getMetadata().getColumnIndex("timestamp");
        while (c.hasNext()) {
            long r = c.next().getDate(ts);
            Assert.assertTrue(r > last);
            last = r;
        }
    }
}
