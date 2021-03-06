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

package com.nfsdb.ql;

import com.nfsdb.JournalWriter;
import com.nfsdb.exceptions.JournalConfigurationException;
import com.nfsdb.exceptions.JournalRuntimeException;
import com.nfsdb.factory.configuration.JournalConfigurationBuilder;
import com.nfsdb.io.RecordSourcePrinter;
import com.nfsdb.io.sink.StringSink;
import com.nfsdb.model.Album;
import com.nfsdb.model.Band;
import com.nfsdb.ql.impl.*;
import com.nfsdb.ql.ops.RecordSourceColumn;
import com.nfsdb.ql.ops.StringConstant;
import com.nfsdb.ql.ops.StringEqualsOperator;
import com.nfsdb.test.tools.JournalTestFactory;
import com.nfsdb.utils.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class JoinStringToSymbolTest {
    @Rule
    public final JournalTestFactory factory;

    private JournalWriter<Band> bw;
    private JournalWriter<Album> aw;

    public JoinStringToSymbolTest() {
        try {
            this.factory = new JournalTestFactory(
                    new JournalConfigurationBuilder() {{
                        $(Band.class)
                                .$sym("name").index()
                                .$sym("type")
                                .$bin("image")
                                .$ts()
                        ;

                        $(Album.class)
                                .$str("band").index()
                                .$sym("name").index()
                                .$ts("releaseDate");

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
    public void testOuterOneToOneHead() throws Exception {

        final String expected = "band1\talbum X\tpop\t1970-01-01T00:00:00.000Z\t1970-01-01T00:00:00.000Z\tband1\thttp://new.band1.com\tjazz\t\n" +
                "band1\talbum BZ\trock\t1970-01-01T00:00:00.000Z\t1970-01-01T00:00:00.000Z\tband1\thttp://new.band1.com\tjazz\t\n" +
                "band3\talbum Y\tmetal\t1970-01-01T00:00:00.000Z\t1970-01-01T00:00:00.000Z\tband3\thttp://band3.com\tjazz\t\n";

        bw.append(new Band().setName("band1").setType("rock").setUrl("http://band1.com"));
        bw.append(new Band().setName("band2").setType("hiphop").setUrl("http://band2.com"));
        bw.append(new Band().setName("band3").setType("jazz").setUrl("http://band3.com"));
        bw.append(new Band().setName("band1").setType("jazz").setUrl("http://new.band1.com"));

        bw.commit();

        aw.append(new Album().setName("album X").setBand("band1").setGenre("pop"));
        aw.append(new Album().setName("album BZ").setBand("band1").setGenre("rock"));
        aw.append(new Album().setName("album Y").setBand("band3").setGenre("metal"));

        aw.commit();

        StatefulJournalSourceImpl master = new StatefulJournalSourceImpl(
                new JournalSource(
                        new JournalPartitionSource(aw, false), new AllRowSource()
                )
        );

        RecordSourceColumn band = new RecordSourceColumn("band", master.getMetadata());
        band.configureSource(master);

        StringSink sink = new StringSink();
        RecordSourcePrinter p = new RecordSourcePrinter(sink);
        p.print(
                new NestedLoopJoinRecordSource(
                        master,
                        new JournalSource(
                                new JournalPartitionSource(bw, false),
                                new KvIndexTopRowSource(
                                        "name"
                                        , new SymByStrLookupKeySource(bw.getSymbolTable("name"), band)
                                        , null
                                ))
                )
        );
//        System.out.println(sink.toString());
        Assert.assertEquals(expected, sink.toString());
    }

    @Test
    public void testOuterOneToOneHeadAndFilter() throws Exception {

        final String expected = "band1\talbum X\tpop\t1970-01-01T00:00:00.000Z\t1970-01-01T00:00:00.000Z\tband1\thttp://old.band1.com\trock\t\n" +
                "band1\talbum BZ\trock\t1970-01-01T00:00:00.000Z\t1970-01-01T00:00:00.000Z\tband1\thttp://old.band1.com\trock\t\n" +
                "band3\talbum Y\tmetal\t1970-01-01T00:00:00.000Z\t1970-01-01T00:00:00.000Z\tnull\t\tnull\t\n";

        bw.append(new Band().setName("band1").setType("rock").setUrl("http://band1.com"));
        bw.append(new Band().setName("band1").setType("rock").setUrl("http://old.band1.com"));
        bw.append(new Band().setName("band2").setType("hiphop").setUrl("http://band2.com"));
        bw.append(new Band().setName("band3").setType("jazz").setUrl("http://band3.com"));
        bw.append(new Band().setName("band1").setType("jazz").setUrl("http://new.band1.com"));

        bw.commit();

        aw.append(new Album().setName("album X").setBand("band1").setGenre("pop"));
        aw.append(new Album().setName("album BZ").setBand("band1").setGenre("rock"));
        aw.append(new Album().setName("album Y").setBand("band3").setGenre("metal"));

        aw.commit();

        StatefulJournalSourceImpl master = new StatefulJournalSourceImpl(
                new JournalSource(
                        new JournalPartitionSource(aw, false), new AllRowSource()
                )
        );

        RecordSourceColumn band = new RecordSourceColumn("band", master.getMetadata());
        band.configureSource(master);

        StringEqualsOperator filter = new StringEqualsOperator();
        filter.setLhs(new RecordSourceColumn("type", bw.getMetadata()));
        filter.setRhs(new StringConstant("rock"));

        StringSink sink = new StringSink();
        RecordSourcePrinter p = new RecordSourcePrinter(sink);
        p.print(
                new NestedLoopJoinRecordSource(
                        master,
                        new JournalSource(
                                new JournalPartitionSource(bw, false),
                                new KvIndexTopRowSource(
                                        "name"
                                        , new SymByStrLookupKeySource(bw.getSymbolTable("name"), band)
                                        , filter
                                ))
                )
        );
        Assert.assertEquals(expected, sink.toString());
    }
}
