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
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.model.Metric;

import java.util.Set;

import static org.hawkular.inventory.api.Relationships.Direction.outgoing;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class ObservableMetrics {
    private ObservableMetrics() {

    }

    public static final class Read extends Notifying<Metrics.Read> implements Metrics.Read {

        Read(Metrics.Read iface, ObserverNotificationStrategy notificationStrategy, Filter[] path) {
            super(iface, notificationStrategy, path);
        }

        @Override
        public Metrics.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), Filter.byTypeAndId(Metric.class, id).get());
        }

        @Override
        public Metrics.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), Filter.byType(Metric.class).and(filters).get());
        }
    }

    public static final class ReadWrite extends Notifying<Metrics.ReadWrite> implements Metrics.ReadWrite {

        ReadWrite(Metrics.ReadWrite iface, ObserverNotificationStrategy notificationStrategy, Filter[] path) {
            super(iface, notificationStrategy, path);
        }

        @Override
        public Metrics.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), Filter.byTypeAndId(Metric.class, id).get());
        }

        @Override
        public Metrics.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), Filter.byType(Metric.class).and(filters).get());
        }

        @Override
        public Metrics.Single create(Metric.Blueprint blueprint) throws EntityAlreadyExistsException {
            return wrapCallAndNotify(Single::new, iface.create(blueprint), new Actions.CreateAction<>(),
                    (fs) -> new Actions.PathContext<>(Metric.class, fs),
                    Filter.byTypeAndId(Metric.class, blueprint.getId()).get());
        }

        @Override
        public void update(Metric metric) throws EntityNotFoundException {
            Actions.PathContext<Metric> ctx = new Actions.PathContext<>(Metric.class, path);
            doAndNotify(iface::update, metric, new Actions.UpdateAction<>(), ctx);
        }

        @Override
        public void delete(String id) throws EntityNotFoundException {
            Actions.PathContext<Metric> ctx = new Actions.PathContext<>(Metric.class, path);
            doAndNotify(iface::delete, id, new Actions.UpdateAction<>(), ctx);
        }
    }

    public static final class ReadRelate extends Notifying<Metrics.ReadRelate> implements Metrics.ReadRelate {

        ReadRelate(Metrics.ReadRelate iface, ObserverNotificationStrategy notificationStrategy, Filter[] path) {
            super(iface, notificationStrategy, path);
        }

        @Override
        public Metrics.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), Filter.byTypeAndId(Metric.class, id).get());
        }

        @Override
        public Metrics.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), Filter.byType(Metric.class).and(filters).get());
        }

        @Override
        public void add(String id) {
            Actions.LinkedAction.RelationshipEnds ctx = new Actions.LinkedAction.RelationshipEnds(
                    Relationships.WellKnown.owns.name(), outgoing, path, get(id).entity());

            doAndNotify(iface::add, id, new Actions.LinkedAction(), ctx);
        }

        @Override
        public void remove(String id) {
            Actions.LinkedAction.RelationshipEnds ctx = new Actions.LinkedAction.RelationshipEnds(
                    Relationships.WellKnown.owns.name(), outgoing, path, get(id).entity());

            doAndNotify(iface::remove, id, new Actions.UnlinkedAction(), ctx);
        }
    }

    public static final class Single extends Notifying.Relatable.Single<Metrics.Single> implements Metrics.Single {

        Single(Metrics.Single iface, ObserverNotificationStrategy notificationStrategy, Filter[] path) {
            super(iface, notificationStrategy, path);
        }

        @Override
        public Metric entity() {
            return iface.entity();
        }
    }

    public static final class Multiple extends Notifying.Relatable.Multiple<Metrics.Multiple>
            implements Metrics.Multiple {

        Multiple(Metrics.Multiple iface, ObserverNotificationStrategy notificationStrategy, Filter[] path) {
            super(iface, notificationStrategy, path);
        }

        @Override
        public Set<Metric> entities() {
            return iface.entities();
        }
    }
}
