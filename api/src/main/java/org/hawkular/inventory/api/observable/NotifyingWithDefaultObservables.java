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
import org.hawkular.inventory.api.filters.Path;
import org.hawkular.inventory.api.model.Entity;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
abstract class NotifyingWithDefaultObservables<WrappedInterface, E extends Entity>
        extends Notifying<WrappedInterface> {

    protected final Class<E> entityType;

    protected NotifyingWithDefaultObservables(WrappedInterface iface, Class<E> entityType, NotificationContext context,
                                              Path path) {
        super(iface, context, path);
        this.entityType = entityType;
    }

    public Observable<Contexts.EntityPath<E>> onCreate() {
        return new ObservableImpl<>(notificationContext, path.extend(Filter.byType(entityType)), Action.CREATE);
    }

    public Observable<Contexts.EntityPath<E>> onUpdate() {
        return new ObservableImpl<>(notificationContext, path.extend(Filter.byType(entityType)), Action.UPDATE);
    }

    public Observable<Contexts.EntityPath<E>> onDelete() {
        return new ObservableImpl<>(notificationContext, path.extend(Filter.byType(entityType)), Action.DELETE);
    }

}
