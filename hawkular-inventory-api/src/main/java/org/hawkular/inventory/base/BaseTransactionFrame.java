/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.inventory.base;

import org.hawkular.inventory.api.Inventory;
import org.hawkular.inventory.api.TransactionFrame;
import org.hawkular.inventory.base.spi.CommitFailureException;
import org.hawkular.inventory.base.spi.InventoryBackend;

/**
 * @author Lukas Krejci
 * @since 0.4.0
 */
final class BaseTransactionFrame<E> implements TransactionFrame {
    private final InventoryBackend<E> origBackend;
    private InventoryBackend.Transaction transaction;
    private Inventory boundInventory;
    private NoncommittingBackend<E> noncommittingBackend;
    private final int maxRetries;
    private final ObservableContext observableContext;

    BaseTransactionFrame(InventoryBackend<E> origBackend, ObservableContext observableContext, int maxRetries) {
        this.origBackend = origBackend;
        this.maxRetries = maxRetries;
        this.observableContext = observableContext;
        reset();
    }

    @Override
    public void commit() throws CommitException {
        try {
            Util.commitOrRetry(transaction, origBackend, null, (t) -> {
                for (PotentiallyCommittingPayload<?> p : noncommittingBackend.getRecordedPayloads()) {
                    t.execute(p);
                }
                return null;
            }, maxRetries);
            origBackend.commit(transaction);
        } catch (CommitFailureException e) {
            throw new CommitException(e);
        }
    }

    @Override
    public void rollback() {
        origBackend.rollback(transaction);
        reset();
    }

    @Override
    public Inventory boundInventory() {
        return boundInventory;
    }

    private void reset() {
        transaction = origBackend.startTransaction(true);
        noncommittingBackend = new NoncommittingBackend<>(origBackend, true);
        boundInventory = new BaseInventory.Initialized<>(noncommittingBackend, observableContext);
    }
}
