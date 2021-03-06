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

package org.nfsdb.examples.query;

import com.nfsdb.Journal;
import com.nfsdb.exceptions.JournalException;
import com.nfsdb.factory.JournalFactory;
import com.nfsdb.query.api.QueryAllBuilder;
import org.nfsdb.examples.model.ModelConfiguration;
import org.nfsdb.examples.model.Price;

import java.util.concurrent.TimeUnit;

public class SimplestSymbolQuery {
    public static void main(String[] args) throws JournalException {
        try (JournalFactory factory = new JournalFactory(ModelConfiguration.CONFIG.build(args[0]))) {
            try (Journal<Price> journal = factory.reader(Price.class)) {
                long tZero = System.nanoTime();
                int count = 0;
                QueryAllBuilder<Price> builder = journal.query().all().withSymValues("sym", "17");

                for (Price p : builder.asResultSet().bufferedIterator()) {
                    assert p != null;
                    count++;
                }

                System.out.println("Read " + count + " objects in " +
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - tZero) + "ms.");
            }
        }
    }
}
