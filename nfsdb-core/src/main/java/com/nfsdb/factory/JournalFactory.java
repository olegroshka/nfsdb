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

package com.nfsdb.factory;

import com.nfsdb.*;
import com.nfsdb.exceptions.JournalException;
import com.nfsdb.factory.configuration.JournalConfiguration;
import com.nfsdb.factory.configuration.JournalConfigurationBuilder;
import com.nfsdb.factory.configuration.JournalMetadata;
import com.nfsdb.factory.configuration.MetadataBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Closeable;

public class JournalFactory extends AbstractJournalReaderFactory implements JournalReaderFactory, JournalWriterFactory, Closeable {

    @SuppressFBWarnings({"SCII_SPOILED_CHILD_INTERFACE_IMPLEMENTOR"})
    public JournalFactory(String journalBase) {
        super(new JournalConfigurationBuilder().build(journalBase));
    }

    public JournalFactory(JournalConfiguration configuration) {
        super(configuration);
    }

    @Override
    public <T> JournalBulkReader<T> bulkReader(JournalKey<T> key) throws JournalException {
        return new JournalBulkReader<>(getOrCreateMetadata(key), key);
    }

    @Override
    public <T> Journal<T> reader(JournalKey<T> key) throws JournalException {
        return new Journal<>(getOrCreateMetadata(key), key);
    }

    @Override
    public <T> JournalBulkWriter<T> bulkWriter(Class<T> clazz) throws JournalException {
        return bulkWriter(new JournalKey<>(clazz));
    }

    @Override
    public <T> JournalBulkWriter<T> bulkWriter(Class<T> clazz, String location) throws JournalException {
        return bulkWriter(new JournalKey<>(clazz, location));
    }

    @Override
    public <T> JournalBulkWriter<T> bulkWriter(Class<T> clazz, String location, int recordHint) throws JournalException {
        return bulkWriter(new JournalKey<>(clazz, location, PartitionType.DEFAULT, recordHint));
    }

    @Override
    public <T> JournalBulkWriter<T> bulkWriter(JournalKey<T> key) throws JournalException {
        return new JournalBulkWriter<>(getConfiguration().createMetadata(key), key);
    }

    @Override
    public <T> JournalWriter<T> writer(Class<T> clazz) throws JournalException {
        return writer(new JournalKey<>(clazz));
    }

    @Override
    public <T> JournalWriter<T> writer(Class<T> clazz, String location) throws JournalException {
        return writer(new JournalKey<>(clazz, location));
    }

    @Override
    public <T> JournalWriter<T> writer(Class<T> clazz, String location, int recordHint) throws JournalException {
        return writer(new JournalKey<>(clazz, location, PartitionType.DEFAULT, recordHint));
    }

    @Override
    public JournalWriter writer(String location) throws JournalException {
        return writer(new JournalKey<>(location));
    }

    @Override
    public <T> JournalWriter<T> writer(JournalKey<T> key) throws JournalException {
        return new JournalWriter<>(getConfiguration().createMetadata(key), key);
    }

    @Override
    public <T> JournalWriter<T> writer(MetadataBuilder<T> b) throws JournalException {
        JournalMetadata<T> metadata = getConfiguration().buildWithRootLocation(b);
        return new JournalWriter<>(metadata, metadata.getKey());
    }

    @Override
    public void close() {
    }
}
