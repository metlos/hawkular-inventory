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
package org.hawkular.inventory.api.observable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public interface ObserverNotificationStrategy {

    <C> void notifySuccess(Observer<C> target, Action action, C context);

    <C> void notifyFailure(Observer<C> target, Throwable failure, Action action, C context);

    class Synchronous implements ObserverNotificationStrategy {

        @Override
        public <C> void notifySuccess(Observer<C> target, Action action, C context) {
            target.onSuccess(action, context);
        }

        @Override
        public <C> void notifyFailure(Observer<C> target, Throwable failure, Action action,
                                      C context) {

            target.onFailure(failure, action, context);
        }
    }

    class Asynchronous implements ObserverNotificationStrategy {
        private static final AtomicInteger COUNTER = new AtomicInteger(0);

        private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(
                (r) -> new Thread(r, "Hawkular Async Inventory Notifications #" + COUNTER.getAndIncrement()));

        @Override
        public <C> void notifySuccess(Observer<C> target, Action action, C context) {
            EXECUTOR.submit(() -> target.onSuccess(action, context));
        }

        @Override
        public <C> void notifyFailure(Observer<C> target, Throwable failure, Action action, C context) {
            EXECUTOR.submit(() -> target.onFailure(failure, action, context));
        }
    }
}
