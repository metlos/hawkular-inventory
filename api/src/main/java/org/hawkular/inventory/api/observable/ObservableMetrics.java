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
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.filters.Path;
import org.hawkular.inventory.api.filters.With;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.observable.Notifying.NotificationContext;

import java.util.Set;

import static org.hawkular.inventory.api.Relationships.Direction.outgoing;
import static org.hawkular.inventory.api.Relationships.WellKnown.owns;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class ObservableMetrics {
    private ObservableMetrics() {

    }

    public static final class Read extends NotifyingWithDefaultObservables<Metrics.Read, Metric>
            implements Metrics.Read {

        Read(Metrics.Read iface, NotificationContext notificationContext, Path path) {
            super(iface, Metric.class, notificationContext, path);
        }

        @Override
        public ObservableMetrics.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), With.id(id));
        }

        @Override
        public ObservableMetrics.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), filters);
        }
    }

    public static final class ReadWrite
            extends NotifyingWithDefaultObservables<Metrics.ReadWrite, Metric>
            implements Metrics.ReadWrite {

        ReadWrite(Metrics.ReadWrite iface, NotificationContext notificationContext, Path path) {
            super(iface, Metric.class, notificationContext, path);
        }

        @Override
        public ObservableMetrics.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), With.id(id));
        }

        @Override
        public ObservableMetrics.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), filters);
        }

        @Override
        public ObservableMetrics.Single create(Metric.Blueprint blueprint) throws EntityAlreadyExistsException {
            return wrapCallAndNotify(Single::new, () -> iface.create(blueprint), Action.CREATE,
                    (fs, s) -> new Contexts.EntityPath<>(Metric.class, fs, s), With.id(blueprint.getId()));
        }

        @Override
        public void update(Metric metric) throws EntityNotFoundException {
            Contexts.EntityPath<Metric> ctx = new Contexts.EntityPath<>(Metric.class, path, metric);
            doAndNotify(iface::update, metric, Action.UPDATE, ctx);
        }

        @Override
        public void delete(String id) throws EntityNotFoundException {
            Contexts.EntityPath<Metric> ctx = new Contexts.EntityPath<>(Metric.class, path, get(id).entity());
            doAndNotify(iface::delete, id, Action.UPDATE, ctx);
        }
    }

    public static final class ReadRelate
            extends NotifyingWithDefaultObservables<Metrics.ReadRelate, Metric>
            implements Metrics.ReadRelate {

        ReadRelate(Metrics.ReadRelate iface, NotificationContext notificationContext, Path path) {
            super(iface, Metric.class, notificationContext, path);
        }

        @Override
        public ObservableMetrics.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), With.id(id));
        }

        @Override
        public ObservableMetrics.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), filters);
        }

        @Override
        public void add(String id) {
            Metrics.Single target = get(id);

            Contexts.RelationshipLink ctx = new Contexts.RelationshipLink(
                    owns.name(), outgoing, path, target.entity());

            doAndNotify(iface::add, id, Action.CREATE, ctx);
        }

        @Override
        public void remove(String id) {
            Contexts.RelationshipLink ctx = new Contexts.RelationshipLink(
                    owns.name(), outgoing, path, get(id).entity());

            doAndNotify(iface::remove, id, Action.DELETE, ctx);
        }

        public Observable<Contexts.RelationshipLink> onAdd() {
            return new ObservableImpl<>(notificationContext, path, Action.CREATE);
        }

        public Observable<Contexts.RelationshipLink> onRemove() {
            return new ObservableImpl<>(notificationContext, path, Action.DELETE);
        }
    }

    public static final class Single extends Notifying.Relatable.Single<Metrics.Single> implements Metrics.Single {

        Single(Metrics.Single iface, NotificationContext notificationContext, Path path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Metric entity() {
            return iface.entity();
        }
    }

    public static final class Multiple extends Notifying.Relatable.Multiple<Metrics.Multiple>
            implements Metrics.Multiple {

        Multiple(Metrics.Multiple iface, NotificationContext notificationContext, Path path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Set<Metric> entities() {
            return iface.entities();
        }
    }
}
