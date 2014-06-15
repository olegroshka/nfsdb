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

package com.nfsdb.journal.query.spi;

import com.nfsdb.journal.Journal;
import com.nfsdb.journal.UnorderedResultSet;
import com.nfsdb.journal.collections.IntArrayList;
import com.nfsdb.journal.column.SymbolTable;
import com.nfsdb.journal.exceptions.JournalException;
import com.nfsdb.journal.query.api.QueryAllBuilder;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.List;

public class QueryAllBuilderImpl<T> implements QueryAllBuilder<T> {

    private final Journal<T> journal;
    private final IntArrayList symbolKeys = new IntArrayList();
    private final List<String> filterSymbols = new ArrayList<>();
    private final IntArrayList filterSymbolKeys = new IntArrayList();
    private String symbol;
    private Interval interval;

    public QueryAllBuilderImpl(Journal<T> journal) {
        this.journal = journal;
    }

    @Override
    public QueryAllBuilder<T> slice(Interval interval) {
        setInterval(interval);
        return this;
    }

    @Override
    public UnorderedResultSet<T> asResultSet() throws JournalException {
        return journal.iteratePartitions(new QueryAllResultSetBuilder<T>(interval, symbol, symbolKeys, filterSymbols, filterSymbolKeys));
    }

    public void setSymbol(String symbol, String... values) {
        this.symbol = symbol;
        SymbolTable symbolTable = journal.getSymbolTable(symbol);
        this.symbolKeys.resetQuick();
        for (String value : values) {
            int key = symbolTable.getQuick(value);
            if (key != SymbolTable.VALUE_NOT_FOUND) {
                symbolKeys.add(key);
            }
        }
    }

    @Override
    public QueryAllBuilder<T> filter(String symbol, String value) {
        SymbolTable tab = journal.getSymbolTable(symbol);
        int key = tab.get(value);
        filterSymbols.add(symbol);
        filterSymbolKeys.add(key);
        return this;
    }

    @Override
    public void resetFilter() {
        filterSymbols.clear();
        filterSymbolKeys.resetQuick();
    }

    public void setInterval(Interval interval) {
        this.interval = interval;
    }
}