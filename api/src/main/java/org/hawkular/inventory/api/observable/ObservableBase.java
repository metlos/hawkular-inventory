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

import org.hawkular.inventory.api.filters.Filter;

import java.util.Set;

/**
 * Basically a slightly modified {@link java.util.Observable}.
 *
 * @author Lukas Krejci
 * @since 0.0.1
 */
public abstract class ObservableBase implements Observable {

    protected final NotificationContext notificationContext;
    protected final Filter[] path;

    protected ObservableBase(NotificationContext context, Filter[] path) {
        this.notificationContext = context;
        this.path = path;
    }

    @Override
    public <C> void subscribe(Observer observer, Action<C> action) {
        notificationContext.storage.addObserver(observer, action, path);
    }

    public <C> void unsubscribe(Observer observer, Action<C> action) {
        notificationContext.storage.removeObserver(observer, action, path);
    }

    protected final <C> void notifyObservers(Throwable failure, Action<C> action, C actionContext) {
        Set<Observer> observers = notificationContext.storage.getObservers(action, path);
        observers.forEach((o) -> {
            if (failure == null) {
                notificationContext.strategy.notifySuccess(this, o, action, actionContext);
            } else {
                notificationContext.strategy.notifyFailure(this, o, failure, action, actionContext);
            }
        });
    }

    protected static class NotificationContext {
        public final ObserverNotificationStrategy strategy;
        public final SharedObserverStorage storage;

        public NotificationContext(SharedObserverStorage storage, ObserverNotificationStrategy strategy) {
            this.storage = storage;
            this.strategy = strategy;
        }
    }
}
