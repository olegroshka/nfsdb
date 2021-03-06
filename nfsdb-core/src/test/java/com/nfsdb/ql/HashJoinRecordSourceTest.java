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
import com.nfsdb.collections.ObjList;
import com.nfsdb.exceptions.JournalConfigurationException;
import com.nfsdb.exceptions.JournalRuntimeException;
import com.nfsdb.factory.configuration.JournalConfigurationBuilder;
import com.nfsdb.io.RecordSourcePrinter;
import com.nfsdb.io.sink.StringSink;
import com.nfsdb.model.Album;
import com.nfsdb.model.Band;
import com.nfsdb.model.Quote;
import com.nfsdb.ql.impl.*;
import com.nfsdb.test.tools.JournalTestFactory;
import com.nfsdb.test.tools.TestUtils;
import com.nfsdb.utils.Files;
import org.junit.*;

public class HashJoinRecordSourceTest {
    @Rule
    public final JournalTestFactory factory;
    private JournalWriter<Band> bw;
    private JournalWriter<Album> aw;

    public HashJoinRecordSourceTest() {
        try {
            this.factory = new JournalTestFactory(
                    new JournalConfigurationBuilder() {{
                        $(Band.class).$ts();
                        $(Album.class).$ts("releaseDate");

                    }}.build(Files.makeTempDir())
            );
        } catch (JournalConfigurationException e) {
            throw new JournalRuntimeException(e);
        }
    }

    @Before
    public void setUp() throws Exception {
        bw = factory.writer(Band.class);
        aw = factory.writer(Album.class);
    }

    @Test
    public void testHashJoinJournalRecordSource() throws Exception {
        bw.append(new Band().setName("band1").setType("rock").setUrl("http://band1.com"));
        bw.append(new Band().setName("band2").setType("blues").setUrl("http://band2.com"));
        bw.append(new Band().setName("band3").setType("jazz").setUrl("http://band3.com"));
        bw.append(new Band().setName("band1").setType("jazz").setUrl("http://new.band1.com"));

        bw.commit();

        aw.append(new Album().setName("album X").setBand("band1").setGenre("pop"));
        aw.append(new Album().setName("album Y").setBand("band3").setGenre("metal"));
        aw.append(new Album().setName("album BZ").setBand("band1").setGenre("rock"));

        aw.commit();

        StringSink sink = new StringSink();
        RecordSourcePrinter p = new RecordSourcePrinter(sink);
        RecordSource<? extends Record> joinResult = new SelectedColumnsRecordSource(
                new HashJoinJournalRecordSource(
                        new JournalSource(new JournalPartitionSource(bw, false), new AllRowSource()),
                        new ObjList<String>() {{
                            add("name");
                        }},
                        new JournalSource(new JournalPartitionSource(aw, false), new AllRowSource()),
                        new ObjList<String>() {{
                            add("band");
                        }}
                ),
                new ObjList<String>() {{
                    add("genre");
                }}
        );
        p.print(joinResult);
        Assert.assertEquals("rock\npop\nmetal\nrock\npop\n", sink.toString());
    }

    @Test
    @Ignore
    public void testHashJoinPerformance() throws Exception {
        JournalWriter<Quote> w1 = factory.writer(Quote.class, "q1");
        TestUtils.generateQuoteData(w1, 100000);

        JournalWriter<Quote> w2 = factory.writer(Quote.class, "q2");
        TestUtils.generateQuoteData(w2, 100000);

        RecordSource<Record> j = new HashJoinJournalRecordSource(
                new JournalSource(new JournalPartitionSource(w1, false), new AllRowSource()),
                new ObjList<String>() {{
                    add("sym");
                }},
                new JournalSource(new JournalPartitionSource(w2, false), new AllRowSource()),
                new ObjList<String>() {{
                    add("sym");
                }}
        );

        long t = System.currentTimeMillis();
        int count = 0;
//        ExportManager.export(j, new File("c:/temp/join.csv"), TextFileFormat.TAB);
        RecordCursor<Record> c = j.prepareCursor(factory);
        while (c.hasNext()) {
            c.next();
            count++;
        }
        System.out.println(System.currentTimeMillis() - t);
        System.out.println(count);


//        ExportManager.export(factory, "q1", new File("d:/q1.csv"), TextFileFormat.TAB);
//        ExportManager.export(factory, "q2", new File("d:/q2.csv"), TextFileFormat.TAB);
//        ExportManager.export(j, new File("d:/join.csv"), TextFileFormat.TAB);
    }

    @Test
    public void testHashJoinRecordSource() throws Exception {
        bw.append(new Band().setName("band1").setType("rock").setUrl("http://band1.com"));
        bw.append(new Band().setName("band2").setType("blues").setUrl("http://band2.com"));
        bw.append(new Band().setName("band3").setType("jazz").setUrl("http://band3.com"));
        bw.append(new Band().setName("band1").setType("jazz").setUrl("http://new.band1.com"));

        bw.commit();

        aw.append(new Album().setName("album X").setBand("band1").setGenre("pop"));
        aw.append(new Album().setName("album Y").setBand("band3").setGenre("metal"));
        aw.append(new Album().setName("album BZ").setBand("band1").setGenre("rock"));

        aw.commit();

        StringSink sink = new StringSink();
        RecordSourcePrinter p = new RecordSourcePrinter(sink);
        RecordSource<? extends Record> joinResult = new SelectedColumnsRecordSource(
                new HashJoinRecordSource(
                        new JournalSource(new JournalPartitionSource(bw, false), new AllRowSource()),
                        new ObjList<String>() {{
                            add("name");
                        }},
                        new JournalSource(new JournalPartitionSource(aw, false), new AllRowSource()),
                        new ObjList<String>() {{
                            add("band");
                        }}
                ),
                new ObjList<String>() {{
                    add("genre");
                }}
        );
        p.print(joinResult);
        Assert.assertEquals("rock\npop\nmetal\nrock\npop\n", sink.toString());
    }
}
