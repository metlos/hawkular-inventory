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
import org.hawkular.inventory.api.MetricTypes;
import org.hawkular.inventory.api.RelationNotFoundException;
import org.hawkular.inventory.api.Relationships;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.filters.Path;
import org.hawkular.inventory.api.filters.With;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.MetricType;
import org.hawkular.inventory.api.model.Relationship;
import org.hawkular.inventory.api.observable.Notifying.NotificationContext;

import java.util.Set;

import static org.hawkular.inventory.api.Relationships.Direction.outgoing;
import static org.hawkular.inventory.api.Relationships.WellKnown.defines;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class ObservableMetricTypes {

    private ObservableMetricTypes() {

    }

    public static final class Read
            extends NotifyingWithDefaultObservables<MetricTypes.Read, MetricType>
            implements MetricTypes.Read {

        Read(MetricTypes.Read iface, NotificationContext notificationContext, Path path) {
            super(iface, MetricType.class, notificationContext, path);
        }

        @Override
        public ObservableMetricTypes.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), With.id(id));
        }

        @Override
        public ObservableMetricTypes.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), Filter.byType(MetricType.class).and(filters).get());
        }
    }

    public static final class ReadWrite
            extends NotifyingWithDefaultObservables<MetricTypes.ReadWrite, MetricType>
            implements MetricTypes.ReadWrite {

        ReadWrite(MetricTypes.ReadWrite iface, NotificationContext notificationContext, Path path) {
            super(iface, MetricType.class, notificationContext, path);
        }

        @Override
        public ObservableMetricTypes.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), With.id(id));
        }

        @Override
        public ObservableMetricTypes.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), filters);
        }

        @Override
        public ObservableMetricTypes.Single create(MetricType.Blueprint blueprint) throws EntityAlreadyExistsException {
            return wrapCallAndNotify(Single::new, () -> iface.create(blueprint), Action.CREATE,
                    (fs, s) -> new Contexts.EntityPath<>(MetricType.class, fs, s),
                    Filter.byTypeAndId(MetricType.class, blueprint.getId()).get());
        }

        @Override
        public void update(MetricType metricType) throws EntityNotFoundException {
            Contexts.EntityPath<MetricType> ctx = new Contexts.EntityPath<>(MetricType.class, path, metricType);
            doAndNotify(iface::update, metricType, Action.UPDATE, ctx);
        }

        @Override
        public void delete(String id) throws EntityNotFoundException {
            Contexts.EntityPath<MetricType> ctx = new Contexts.EntityPath<>(MetricType.class, path, get(id).entity());
            doAndNotify(iface::delete, id, Action.DELETE, ctx);
        }
    }

    public static final class ReadRelate
            extends NotifyingWithDefaultObservables<MetricTypes.ReadRelate, MetricType>
            implements MetricTypes.ReadRelate {

        ReadRelate(MetricTypes.ReadRelate iface, NotificationContext notificationContext, Path path) {
            super(iface, MetricType.class, notificationContext, path);
        }

        @Override
        public ObservableMetricTypes.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), With.id(id));
        }

        @Override
        public ObservableMetricTypes.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), filters);
        }

        @Override
        public void add(String id) {
            Contexts.RelationshipLink ctx = new Contexts.RelationshipLink(
                    Relationships.WellKnown.owns.name(), outgoing, path, get(id).entity());

            doAndNotify(iface::add, id, Action.LINK, ctx);
        }

        @Override
        public void remove(String id) {
            Contexts.RelationshipLink ctx = new Contexts.RelationshipLink(
                    Relationships.WellKnown.owns.name(), outgoing, path, get(id).entity());

            doAndNotify(iface::remove, id, Action.UNLINK, ctx);
        }

        public Observable<Contexts.EntityPath<Relationship>> onAdd() {
            return new ObservableImpl<>(notificationContext, path, Action.CREATE);
        }

        public Observable<Contexts.EntityPath<Relationship>> onRemove() {
            return new ObservableImpl<>(notificationContext, path, Action.DELETE);
        }
    }

    public static final class Single extends Notifying.Relatable.Single<MetricTypes.Single>
            implements MetricTypes.Single {

        Single(MetricTypes.Single iface, NotificationContext notificationContext, Path path) {
            super(iface, notificationContext, path);
        }

        @Override
        public ObservableMetrics.Read metrics() {
            return wrapCall(ObservableMetrics.Read::new, iface.metrics(), Filter.relatedBy(defines)
                    .andType(Metric.class).get());
        }

        @Override
        public MetricType entity() {
            return iface.entity();
        }
    }

    public static final class Multiple extends Notifying.Relatable.Multiple<MetricTypes.Multiple>
            implements MetricTypes.Multiple {

        Multiple(MetricTypes.Multiple iface, NotificationContext notificationContext, Path path) {
            super(iface, notificationContext, path);
        }

        @Override
        public ObservableMetrics.Read metrics() {
            return wrapCall(ObservableMetrics.Read::new, iface.metrics(), Filter.relatedBy(defines)
                    .andType(Metric.class).get());
        }

        @Override
        public Set<MetricType> entities() {
            return iface.entities();
        }
    }
}
