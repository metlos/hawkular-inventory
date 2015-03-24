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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Basically a slightly modified {@link java.util.Observable}.
 *
 * @author Lukas Krejci
 * @since 0.0.1
 */
public abstract class ObservableBase implements Observable {

    private Map<Action<?>, Set<Observer>> observerMap = new HashMap<>();
    protected final ObserverNotificationStrategy notificationStrategy;

    protected ObservableBase(ObserverNotificationStrategy notificationStrategy) {
        this.notificationStrategy = notificationStrategy;
    }

    @Override
    public <C> void subscribe(Observer observer, Action<C> action) {
        Set<Observer> observers = observerMap.get(action);
        if (observers == null) {
            observers = new HashSet<>();
            observerMap.put(action, observers);
        }

        observers.add(observer);
    }

    public <C> void unsubscribe(Observer observer, Action<C> action) {
        Set<Observer> observers = observerMap.get(action);
        if (observers == null) {
            return;
        }

        observers.remove(observer);
    }

    protected final <C> void notifyObservers(Throwable failure, Action<C> action, C actionContext) {
        Set<Observer> observers = observerMap.get(action);

        if (observers != null) {
            observers.forEach((o) -> {
                if (failure == null) {
                    notificationStrategy.notifySuccess(this, o, action, actionContext);
                } else {
                    notificationStrategy.notifyFailure(this, o, failure, action, actionContext);
                }
            });
        }
    }
}
