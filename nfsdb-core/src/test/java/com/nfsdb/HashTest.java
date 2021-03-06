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

package com.nfsdb;

import com.nfsdb.collections.IntHashSet;
import com.nfsdb.utils.Hash;
import com.nfsdb.utils.Rnd;
import com.nfsdb.utils.Unsafe;
import org.junit.Assert;
import org.junit.Test;

public class HashTest {

    @Test
    public void testStringHash() throws Exception {
        Rnd rnd = new Rnd();
        IntHashSet hashes = new IntHashSet(100000);
        final int LEN = 30;

        long address = Unsafe.getUnsafe().allocateMemory(LEN);

        for (int i = 0; i < 100000; i++) {
            rnd.nextChars(address, LEN);
            hashes.add(Hash.hashMem(address, LEN));
        }
        Assert.assertTrue("Hash function distribution dropped", hashes.size() > 99990);
    }
}
