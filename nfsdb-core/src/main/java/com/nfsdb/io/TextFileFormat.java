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

package com.nfsdb.io;

import com.nfsdb.exceptions.JournalRuntimeException;
import com.nfsdb.io.parser.CsvParser;
import com.nfsdb.io.parser.PipeParser;
import com.nfsdb.io.parser.TabParser;
import com.nfsdb.io.parser.TextParser;

public enum TextFileFormat {
    CSV(','), TAB('\t'), PIPE('|');

    private final char delimiter;


    TextFileFormat(char delimiter) {
        this.delimiter = delimiter;
    }

    public char getDelimiter() {
        return delimiter;
    }

    public TextParser newParser() {
        switch (this) {
            case CSV:
                return new CsvParser();
            case TAB:
                return new TabParser();
            case PIPE:
                return new PipeParser();
            default:
                throw new JournalRuntimeException("Unknown file format: " + this);
        }
    }
}
