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

package com.nfsdb.ha;

import com.nfsdb.Journal;
import com.nfsdb.JournalKey;
import com.nfsdb.collections.IntIntHashMap;
import com.nfsdb.collections.IntList;
import com.nfsdb.collections.ObjList;
import com.nfsdb.exceptions.JournalDisconnectedChannelException;
import com.nfsdb.exceptions.JournalException;
import com.nfsdb.exceptions.JournalNetworkException;
import com.nfsdb.factory.configuration.JournalConfiguration;
import com.nfsdb.ha.auth.AuthorizationHandler;
import com.nfsdb.ha.bridge.JournalEvent;
import com.nfsdb.ha.bridge.JournalEventHandler;
import com.nfsdb.ha.bridge.JournalEventProcessor;
import com.nfsdb.ha.comsumer.JournalClientStateConsumer;
import com.nfsdb.ha.config.ServerConfig;
import com.nfsdb.ha.model.Command;
import com.nfsdb.ha.model.IndexedJournalKey;
import com.nfsdb.ha.model.JournalClientState;
import com.nfsdb.ha.producer.HugeBufferProducer;
import com.nfsdb.ha.producer.JournalDeltaProducer;
import com.nfsdb.ha.protocol.CommandConsumer;
import com.nfsdb.ha.protocol.CommandProducer;
import com.nfsdb.ha.protocol.Version;
import com.nfsdb.ha.protocol.commands.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.net.SocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.WritableByteChannel;

@SuppressFBWarnings({"PL_PARALLEL_LISTS", "LII_LIST_INDEXED_ITERATING", "LII_LIST_INDEXED_ITERATING"})
public class JournalServerAgent {

    private static final byte JOURNAL_INDEX_NOT_FOUND = -1;
    private final IntIntHashMap writerToReaderMap = new IntIntHashMap();
    private final IntList readerToWriterMap = new IntList();
    private final JournalServer server;
    private final CommandConsumer commandConsumer = new CommandConsumer();
    private final CommandProducer commandProducer = new CommandProducer();
    private final SetKeyRequestConsumer setKeyRequestConsumer = new SetKeyRequestConsumer();
    private final StringResponseProducer stringResponseProducer = new StringResponseProducer();
    private final JournalClientStateConsumer journalClientStateConsumer = new JournalClientStateConsumer();
    private final IntResponseProducer intResponseProducer = new IntResponseProducer();
    private final IntResponseConsumer intResponseConsumer = new IntResponseConsumer();
    private final ObjList<Journal> readers = new ObjList<>();
    private final ObjList<JournalDeltaProducer> producers = new ObjList<>();
    private final ObjList<JournalClientState> clientStates = new ObjList<>();
    private final StatsCollectingWritableByteChannel statsChannel;
    private final JournalEventProcessor eventProcessor;
    private final EventHandler handler = new EventHandler();
    private final AuthorizationHandler authorizationHandler;
    private final ByteArrayResponseConsumer byteArrayResponseConsumer = new ByteArrayResponseConsumer();
    private final SocketAddress socketAddress;
    private boolean authorized;

    public JournalServerAgent(JournalServer server, SocketAddress socketAddress, AuthorizationHandler authorizationHandler) {
        this.server = server;
        this.socketAddress = socketAddress;
        this.statsChannel = new StatsCollectingWritableByteChannel(socketAddress);
        this.eventProcessor = new JournalEventProcessor(server.getBridge());
        this.authorizationHandler = authorizationHandler;
        this.authorized = authorizationHandler == null;
        readerToWriterMap.zero(JOURNAL_INDEX_NOT_FOUND);
    }

    public void close() {
        server.getBridge().removeAgentSequence(eventProcessor.getSequence());
        journalClientStateConsumer.free();
        commandConsumer.free();
        setKeyRequestConsumer.free();
        intResponseConsumer.free();
        byteArrayResponseConsumer.free();
        commandProducer.free();
        stringResponseProducer.free();
        intResponseProducer.free();
        for (int i = 0, k = producers.size(); i < k; i++) {
            producers.getQuick(i).free();
        }
    }

    public void process(ByteChannel channel) throws JournalNetworkException {
        commandConsumer.read(channel);
        switch (commandConsumer.getValue()) {
            case SET_KEY_CMD:
                setClientKey(channel);
                break;
            case DELTA_REQUEST_CMD:
                checkAuthorized(channel);
                log(ServerLogMsg.Level.TRACE, "DeltaRequest command received");
                journalClientStateConsumer.read(channel);
                storeDeltaRequest(channel, journalClientStateConsumer.getValue());
                break;
            case CLIENT_READY_CMD:
                checkAuthorized(channel);
                statsChannel.setDelegate(channel);
                dispatch(statsChannel);
                statsChannel.logStats();
                break;
            case CLIENT_DISCONNECT:
                throw new JournalDisconnectedChannelException();
            case PROTOCOL_VERSION:
                checkProtocolVersion(channel, intResponseConsumer.getValue(channel));
                break;
            case HANDSHAKE_COMPLETE:
                if (authorized) {
                    ok(channel);
                } else {
                    stringResponseProducer.write(channel, "AUTH");
                }
                break;
            case AUTHORIZATION:
                byteArrayResponseConsumer.read(channel);
                authorize(channel, byteArrayResponseConsumer.getValue());
                break;
            case ELECTION:
                server.handleElectionMessage(channel);
                break;
            case ELECTED:
                server.handleElectedMessage(channel);
                break;
            default:
                throw new JournalNetworkException("Corrupt channel");
        }
    }

    @SuppressFBWarnings({"PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS"})
    private void authorize(WritableByteChannel channel, byte[] value) throws JournalNetworkException {
        if (!authorized) {
            try {
                int k = readers.size();
                ObjList<JournalKey> keys = new ObjList<>(k);
                for (int i = 0; i < k; i++) {
                    keys.add(readers.getQuick(i).getKey());
                }
                authorized = authorizationHandler.isAuthorized(value, keys);
            } catch (Throwable e) {
                log(ServerLogMsg.Level.ERROR, "Exception in authorization handler:", e);
                authorized = false;
            }
        }

        if (authorized) {
            ok(channel);
        } else {
            error(channel, "Authorization failed");
        }
    }

    private void checkAuthorized(WritableByteChannel channel) throws JournalNetworkException {
        if (!authorized) {
            error(channel, "NOT AUTHORIZED");
            throw new JournalDisconnectedChannelException();
        }
    }

    private void checkProtocolVersion(ByteChannel channel, int version) throws JournalNetworkException {
        if (version == Version.PROTOCOL_VERSION) {
            ok(channel);
        } else {
            error(channel, "Unsupported protocol version. Client: " + version + ", Server: " + Version.PROTOCOL_VERSION);
        }
    }

    private <T> void createReader(int index, JournalKey<T> key) throws JournalException {
        Journal journal = readers.getQuiet(index);
        if (journal == null) {
            readers.extendAndSet(index, journal = server.getFactory().reader(key));
        }

        JournalDeltaProducer producer = producers.getQuiet(index);
        if (producer == null) {
            producers.extendAndSet(index, new JournalDeltaProducer(journal));
        }
    }

    private void dispatch(WritableByteChannel channel) throws JournalNetworkException {
        if (!processJournalEvents(channel, false)) {
            processJournalEvents(channel, true);
        }
    }

    private boolean dispatch0(WritableByteChannel channel, int journalIndex) {
        long time = System.currentTimeMillis();
        JournalClientState state = clientStates.get(journalIndex);

        // x1 is clientStateValid
        // x2 is writerUpdateReceived
        // x3 is clientStateSynchronised
        // 1 is true
        // 0 is false
        //
        // x1 = 1 && x3 = 0 -> send (brand new, unvalidated request)
        // x1 = 0 - don't send (not client state, don't know what to send)
        // x1 = 1 && x2 = 1 ->  send (either new request, or previously validated but not sent with and update)

        if (state == null || state.isClientStateInvalid() ||
                (state.isWaitingOnEvents() && time - state.getClientStateSyncTime() <= ServerConfig.SYNC_TIMEOUT)) {
            return false;
        }


        try {
            boolean dataSent = dispatchProducer(channel, state.getTxn(), state.getTxPin(), getProducer(journalIndex), journalIndex);
            if (dataSent) {
                state.invalidateClientState();
            } else {
                state.setClientStateSyncTime(time);
            }
            return dataSent;
        } catch (Exception e) {
            server.getLogger().msg()
                    .setLevel(ServerLogMsg.Level.ERROR)
                    .setSocketAddress(socketAddress)
                    .setMessage("Client appears to be refusing new data from server, corrupt client")
                    .setException(e)
                    .send();
            return false;
        }
    }

    private boolean dispatchProducer(
            WritableByteChannel channel
            , long txn
            , long txPin
            , JournalDeltaProducer journalDeltaProducer
            , int index) throws JournalNetworkException, JournalException {

        journalDeltaProducer.configure(txn, txPin);
        if (journalDeltaProducer.hasContent()) {
            server.getLogger().msg().setMessage("Sending data").setSocketAddress(socketAddress).send();
            commandProducer.write(channel, Command.JOURNAL_DELTA_CMD);
            intResponseProducer.write(channel, index);
            journalDeltaProducer.write(channel);
            return true;
        }
        return false;

    }

    private void error(WritableByteChannel channel, String message) throws JournalNetworkException {
        error(channel, message, null);
    }

    private void error(WritableByteChannel channel, String message, Exception e) throws JournalNetworkException {
        stringResponseProducer.write(channel, message);
        server.getLogger().msg()
                .setLevel(ServerLogMsg.Level.INFO)
                .setSocketAddress(socketAddress)
                .setMessage(message)
                .setException(e)
                .send();
    }

    private JournalDeltaProducer getProducer(int index) {
        return producers.get(index);
    }

    private void log(ServerLogMsg.Level level, String msg) {
        log(level, msg, null);
    }

    private void log(ServerLogMsg.Level level, String msg, Throwable e) {
        server.getLogger().msg()
                .setLevel(level)
                .setSocketAddress(socketAddress)
                .setMessage(msg)
                .setException(e)
                .send();
    }

    private void ok(WritableByteChannel channel) throws JournalNetworkException {
        stringResponseProducer.write(channel, "OK");
    }

    private boolean processJournalEvents(final WritableByteChannel channel, boolean blocking) throws JournalNetworkException {

        handler.setChannel(channel);
        boolean dataSent = false;
        if (eventProcessor.process(handler, blocking)) {
            dataSent = handler.isDataSent();

            // handler would have dispatched those journals, which received updates
            // this loop does two things:
            // 1. attempts to dispatch0 journals that didn't receive updates, dispatch0 method would check timeout and decide.
            // 2. reset writer update received status
            for (int i = 0, k = clientStates.size(); i < k; i++) {
                JournalClientState state = clientStates.getQuick(i);
                if (state.isWaitingOnEvents()) {
                    dataSent = dispatch0(channel, i) || dataSent;
                }
                state.setWaitingOnEvents(true);
            }

            if (dataSent) {
                commandProducer.write(channel, Command.SERVER_READY_CMD);
            } else if (blocking) {
                server.getLogger().msg()
                        .setLevel(ServerLogMsg.Level.INFO)
                        .setSocketAddress(socketAddress)
                        .setMessage("Client appears to be refusing new data from server, corrupt client")
                        .send();
            }
        } else {
            if (server.isRunning()) {
                commandProducer.write(channel, Command.SERVER_HEARTBEAT);
                server.getLogger().msg()
                        .setLevel(ServerLogMsg.Level.TRACE)
                        .setSocketAddress(socketAddress)
                        .setMessage("Heartbeat")
                        .send();
            } else {
                commandProducer.write(channel, Command.SERVER_SHUTDOWN);
            }
        }
        return dataSent;
    }

    private void sendMetadata(WritableByteChannel channel, int index) throws JournalException, JournalNetworkException {
        try (HugeBufferProducer h = new HugeBufferProducer(new File(readers.get(index).getLocation(), JournalConfiguration.FILE_NAME))) {
            h.write(channel);
        }
    }

    @SuppressWarnings("unchecked")
    private void setClientKey(ByteChannel channel) throws JournalNetworkException {
        log(ServerLogMsg.Level.TRACE, "SetKey command received");
        setKeyRequestConsumer.read(channel);
        IndexedJournalKey indexedKey = setKeyRequestConsumer.getValue();

        JournalKey<?> readerKey = indexedKey.getKey();
        int index = indexedKey.getIndex();

        IndexedJournalKey augmentedReaderKey = server.getWriterIndex0(readerKey);
        if (augmentedReaderKey == null) {
            error(channel, "Requested key not exported: " + readerKey);
        } else {
            writerToReaderMap.put(augmentedReaderKey.getIndex(), index);
            readerToWriterMap.extendAndSet(index, augmentedReaderKey.getIndex());
            try {
                createReader(index, augmentedReaderKey.getKey());
                ok(channel);
                sendMetadata(channel, index);
            } catch (JournalException e) {
                error(channel, "Could not created reader for key: " + readerKey, e);
            }
        }
    }

    private void storeDeltaRequest(WritableByteChannel channel, JournalClientState request) throws JournalNetworkException {
        int index = request.getJournalIndex();

        if (readerToWriterMap.getQuiet(index) == JOURNAL_INDEX_NOT_FOUND) {
            error(channel, "Journal index does not match key request");
        } else {
            JournalClientState r = clientStates.getQuiet(index);
            if (r == null) {
                r = new JournalClientState();
                clientStates.extendAndSet(index, r);
            }
            request.deepCopy(r);
            r.invalidateClientState();
            r.setClientStateSyncTime(0);
            r.setWaitingOnEvents(true);

            ok(channel);
        }
    }

    private class EventHandler implements JournalEventHandler {

        private WritableByteChannel channel;
        private boolean dataSent = false;

        @Override
        public void handle(JournalEvent event) {
            int journalIndex = writerToReaderMap.get(event.getIndex());
            if (journalIndex != JOURNAL_INDEX_NOT_FOUND) {
                JournalClientState status = clientStates.get(journalIndex);
                if (status != null && status.isWaitingOnEvents()) {
                    status.setWaitingOnEvents(false);
                    dataSent = dispatch0(channel, journalIndex) || dataSent;
                }
            }
        }

        public boolean isDataSent() {
            return dataSent;
        }

        public void setChannel(WritableByteChannel channel) {
            this.channel = channel;
            this.dataSent = false;
        }
    }
}
