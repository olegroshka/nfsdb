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

package com.nfsdb.io.parser.listener.probe;

import com.nfsdb.io.ImportedColumnMetadata;
import com.nfsdb.io.ImportedColumnType;
import com.nfsdb.storage.ColumnType;
import com.nfsdb.utils.Dates;

public class DateFmt2Probe implements TypeProbe {
    @Override
    public ImportedColumnMetadata getMetadata() {
        ImportedColumnMetadata m = new ImportedColumnMetadata();
        m.type = ColumnType.DATE;
        m.importedType = ImportedColumnType.DATE_2;
        m.size = 8;
        return m;
    }

    @Override
    public boolean probe(CharSequence seq) {
        try {
            Dates.parseDateTimeFmt2(seq);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
