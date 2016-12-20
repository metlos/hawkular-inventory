/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.inventory.impl.tinkerpop.provider;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.util.AbstractTransaction;

/**
 * @author Lukas Krejci
 * @since 1.2.0
 */
final class LockingTransaction extends AbstractTransaction {
    private final ReentrantReadWriteLock txLock;

    public LockingTransaction(TransactionLockingGraph graph, ReentrantReadWriteLock txLock) {
        super(graph);
        this.txLock = txLock;
    }

    public void lockForWriting() {
        boolean lockForRead = txLock.getReadLockCount() > 0;
        boolean lockedForWrite = txLock.getWriteHoldCount() > 0;

        if (!lockForRead && !lockedForWrite) {
            throw new IllegalStateException("No transaction active.");
        }

        if (lockForRead) {
            txLock.readLock().unlock();
        }

        if (!lockedForWrite) {
            txLock.writeLock().lock();
        }
    }

    @Override protected void doOpen() {
        try {
            txLock.readLock().tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GraphLockedException();
        }

        if (txLock.getReadHoldCount() > 1) {
            txLock.readLock().unlock();
            throw new IllegalStateException("Nested transaction detected");
        }
    }

    @Override protected void doCommit() throws TransactionException {
    }

    @Override protected void doRollback() throws TransactionException {
    }

    @Override protected void doClose() {
        if (txLock.getWriteHoldCount() == 1) {
            txLock.writeLock().unlock();
        } else if (txLock.getReadHoldCount() == 1) {
            txLock.readLock().unlock();
        } else {
            throw new IllegalStateException("No transaction active");
        }
    }

    @Override protected void doReadWrite() {
    }

    @Override protected void fireOnCommit() {
    }

    @Override protected void fireOnRollback() {
    }

    @Override public Transaction onReadWrite(Consumer<Transaction> consumer) {
        return this;
    }

    @Override public Transaction onClose(Consumer<Transaction> consumer) {
        return this;
    }

    @Override public void addTransactionListener(Consumer<Status> listener) {
    }

    @Override public void removeTransactionListener(Consumer<Status> listener) {
    }

    @Override public void clearTransactionListeners() {
    }

    @Override public boolean isOpen() {
        return txLock.getReadHoldCount() > 0 || txLock.getWriteHoldCount() > 0;
    }
}
