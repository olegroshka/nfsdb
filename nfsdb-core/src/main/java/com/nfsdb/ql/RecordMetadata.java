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

import com.nfsdb.factory.configuration.RecordColumnMetadata;

public interface RecordMetadata {
    RecordColumnMetadata getColumn(int index);

    RecordColumnMetadata getColumn(CharSequence name);

    int getColumnCount();

    /**
     * Finds index of column by given name. If name is invalid a JournalRuntimeException is thrown.
     *
     * @param name column name
     * @return column index between 0 and getColumnCount()-1
     */
    int getColumnIndex(CharSequence name);

    boolean invalidColumn(CharSequence name);
}
