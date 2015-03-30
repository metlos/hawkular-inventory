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

import org.hawkular.inventory.api.filters.Path;

/**
 * Basically a slightly modified {@link java.util.Observable}.
 *
 * @author Lukas Krejci
 * @since 0.0.1
 */
final class ObservableImpl<C> implements Observable<C> {

    private final Notifying.NotificationContext notificationContext;
    private final Path path;
    private final Action action;

    ObservableImpl(Notifying.NotificationContext context, Path path, Action action) {
        this.notificationContext = context;
        this.path = path;
        this.action = action;
    }

    @Override
    public void subscribe(Observer<C> observer) {
        notificationContext.storage.addObserver(observer, action, path);
    }

    @Override
    public void unsubscribe(Observer<C> observer) {
        notificationContext.storage.removeObserver(observer, action, path);
    }

}
