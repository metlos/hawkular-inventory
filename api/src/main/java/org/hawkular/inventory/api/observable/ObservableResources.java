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
import org.hawkular.inventory.api.Metrics;
import org.hawkular.inventory.api.RelationNotFoundException;
import org.hawkular.inventory.api.Relationships;
import org.hawkular.inventory.api.Resources;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.model.Resource;

import java.util.Set;

import static org.hawkular.inventory.api.Relationships.WellKnown.owns;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class ObservableResources {

    private ObservableResources() {

    }

    public static final class Read extends Notifying<Resources.Read> implements Resources.Read {

        Read(Resources.Read iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Resources.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), Filter.byTypeAndId(Resource.class, id).get());
        }

        @Override
        public Resources.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), Filter.byType(Resource.class).and(filters).get());
        }
    }

    public static final class ReadWrite extends Notifying<Resources.ReadWrite> implements Resources.ReadWrite {

        ReadWrite(Resources.ReadWrite iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Resources.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), Filter.byTypeAndId(Resource.class, id).get());
        }

        @Override
        public Resources.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), Filter.byType(Resource.class).and(filters).get());
        }

        @Override
        public Resources.Single create(Resource.Blueprint blueprint) throws EntityAlreadyExistsException {
            return wrapCallAndNotify(Single::new, iface.create(blueprint), new Actions.CreateAction<>(),
                    (fs) -> new Actions.PathContext<>(Resource.class, fs),
                    Filter.byTypeAndId(Resource.class, blueprint.getId()).get());
        }

        @Override
        public void update(Resource resource) throws EntityNotFoundException {
            Actions.PathContext<Resource> ctx = new Actions.PathContext<>(Resource.class, path);
            doAndNotify(iface::update, resource, new Actions.UpdateAction<>(), ctx);
        }

        @Override
        public void delete(String id) throws EntityNotFoundException {
            Actions.PathContext<Resource> ctx = new Actions.PathContext<>(Resource.class, path);
            doAndNotify(iface::delete, id, new Actions.DeleteAction<>(), ctx);
        }
    }

    public static final class Single extends Notifying.Relatable.Single<Resources.Single> implements Resources.Single {

        Single(Resources.Single iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Metrics.ReadRelate metrics() {
            return wrapCall(ObservableMetrics.ReadRelate::new, iface.metrics(), Filter.relatedBy(owns).get());
        }

        @Override
        public Resource entity() {
            return iface.entity();
        }
    }

    public static final class Multiple extends Notifying.Relatable.Multiple<Resources.Multiple>
            implements Resources.Multiple {

        Multiple(Resources.Multiple iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Metrics.Read metrics() {
            return wrapCall(ObservableMetrics.Read::new, iface.metrics(), Filter.relatedBy(owns).get());
        }

        @Override
        public Set<Resource> entities() {
            return iface.entities();
        }
    }
}
