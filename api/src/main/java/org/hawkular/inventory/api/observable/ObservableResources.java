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

import org.hawkular.inventory.api.EntityAlreadyExistsException;
import org.hawkular.inventory.api.EntityNotFoundException;
import org.hawkular.inventory.api.RelationNotFoundException;
import org.hawkular.inventory.api.Resources;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.filters.Path;
import org.hawkular.inventory.api.filters.With;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.observable.Notifying.NotificationContext;

import java.util.Set;

import static org.hawkular.inventory.api.Relationships.WellKnown.owns;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class ObservableResources {

    private ObservableResources() {

    }

    public static final class Read
            extends NotifyingWithDefaultObservables<Resources.Read, Resource>
            implements Resources.Read {

        Read(Resources.Read iface, NotificationContext notificationContext, Path path) {
            super(iface, Resource.class, notificationContext, path);
        }

        @Override
        public ObservableResources.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), With.id(id));
        }

        @Override
        public ObservableResources.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), filters);
        }
    }

    public static final class ReadWrite
            extends NotifyingWithDefaultObservables<Resources.ReadWrite, Resource>
            implements Resources.ReadWrite {

        ReadWrite(Resources.ReadWrite iface, NotificationContext notificationContext, Path path) {
            super(iface, Resource.class, notificationContext, path);
        }

        @Override
        public ObservableResources.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), With.id(id));
        }

        @Override
        public ObservableResources.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), filters);
        }

        @Override
        public ObservableResources.Single create(Resource.Blueprint blueprint) throws EntityAlreadyExistsException {
            return wrapCallAndNotify(Single::new, () -> iface.create(blueprint), Action.CREATE,
                    (fs, s) -> new Contexts.EntityPath<>(Resource.class, fs, s),
                    Filter.byTypeAndId(Resource.class, blueprint.getId()).get());
        }

        @Override
        public void update(Resource resource) throws EntityNotFoundException {
            Contexts.EntityPath<Resource> ctx = new Contexts.EntityPath<>(Resource.class, path, resource);
            doAndNotify(iface::update, resource, Action.UPDATE, ctx);
        }

        @Override
        public void delete(String id) throws EntityNotFoundException {
            Contexts.EntityPath<Resource> ctx = new Contexts.EntityPath<>(Resource.class, path, get(id).entity());
            doAndNotify(iface::delete, id, Action.DELETE, ctx);
        }
    }

    public static final class Single extends Notifying.Relatable.Single<Resources.Single> implements Resources.Single {

        Single(Resources.Single iface, NotificationContext notificationContext, Path path) {
            super(iface, notificationContext, path);
        }

        @Override
        public ObservableMetrics.ReadRelate metrics() {
            return wrapCall(ObservableMetrics.ReadRelate::new, iface.metrics(), Filter.relatedBy(owns)
                    .andType(Metric.class).get());
        }

        @Override
        public Resource entity() {
            return iface.entity();
        }
    }

    public static final class Multiple extends Notifying.Relatable.Multiple<Resources.Multiple>
            implements Resources.Multiple {

        Multiple(Resources.Multiple iface, NotificationContext notificationContext, Path path) {
            super(iface, notificationContext, path);
        }

        @Override
        public ObservableMetrics.Read metrics() {
            return wrapCall(ObservableMetrics.Read::new, iface.metrics(), Filter.relatedBy(owns)
                    .andType(Metric.class).get());
        }

        @Override
        public Set<Resource> entities() {
            return iface.entities();
        }
    }
}
