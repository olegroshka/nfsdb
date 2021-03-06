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

package com.nfsdb;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.nfsdb.exceptions.JournalRuntimeException;
import com.nfsdb.utils.NamedDaemonThreadFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressFBWarnings({"EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS"})
public class PartitionCleaner {
    private final ExecutorService executor;
    private final RingBuffer<PartitionCleanerEvent> ringBuffer = RingBuffer.createSingleProducer(PartitionCleanerEvent.EVENT_FACTORY, 32, new BlockingWaitStrategy());
    private final BatchEventProcessor<PartitionCleanerEvent> batchEventProcessor;
    private final PartitionCleanerEventHandler h;

    public PartitionCleaner(JournalWriter writer, String name) {
        this.executor = Executors.newCachedThreadPool(new NamedDaemonThreadFactory("nfsdb-journal-cleaner-" + name, true));
        this.batchEventProcessor = new BatchEventProcessor<>(ringBuffer, ringBuffer.newBarrier(), h = new PartitionCleanerEventHandler(writer));
        ringBuffer.addGatingSequences(batchEventProcessor.getSequence());
    }

    public void halt() {
        executor.shutdown();

        try {
            h.startLatch.await();
            batchEventProcessor.halt();
            h.stopLatch.await();
        } catch (InterruptedException e) {
            throw new JournalRuntimeException(e);
        }

        executor.shutdown();
    }

    public void purge() {
        ringBuffer.publish(ringBuffer.next());
    }

    public void start() {
        executor.submit(batchEventProcessor);
    }
}
