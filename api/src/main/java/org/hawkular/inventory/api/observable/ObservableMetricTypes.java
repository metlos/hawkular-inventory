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
import org.hawkular.inventory.api.Metrics;
import org.hawkular.inventory.api.RelationNotFoundException;
import org.hawkular.inventory.api.Relationships;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.model.MetricType;

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

    public static final class Read extends Notifying<MetricTypes.Read> implements MetricTypes.Read {

        Read(MetricTypes.Read iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public MetricTypes.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), Filter.byTypeAndId(MetricType.class, id).get());
        }

        @Override
        public MetricTypes.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), Filter.byType(MetricType.class).and(filters).get());
        }
    }

    public static final class ReadWrite extends Notifying<MetricTypes.ReadWrite> implements MetricTypes.ReadWrite {

        ReadWrite(MetricTypes.ReadWrite iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public MetricTypes.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), Filter.byTypeAndId(MetricType.class, id).get());
        }

        @Override
        public MetricTypes.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), Filter.byType(MetricType.class).and(filters).get());
        }

        @Override
        public MetricTypes.Single create(MetricType.Blueprint blueprint) throws EntityAlreadyExistsException {
            return wrapCallAndNotify(Single::new, iface.create(blueprint), new Actions.CreateAction<>(),
                    (fs) -> new Actions.PathContext<>(MetricType.class, fs),
                    Filter.byTypeAndId(MetricType.class, blueprint.getId()).get());
        }

        @Override
        public void update(MetricType metricType) throws EntityNotFoundException {
            Actions.PathContext<MetricType> ctx = new Actions.PathContext<>(MetricType.class, path);
            doAndNotify(iface::update, metricType, new Actions.UpdateAction<>(), ctx);
        }

        @Override
        public void delete(String id) throws EntityNotFoundException {
            Actions.PathContext<MetricType> ctx = new Actions.PathContext<>(MetricType.class, path);
            doAndNotify(iface::delete, id, new Actions.DeleteAction<>(), ctx);
        }
    }

    public static final class ReadRelate extends Notifying<MetricTypes.ReadRelate> implements MetricTypes.ReadRelate {

        ReadRelate(MetricTypes.ReadRelate iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public MetricTypes.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), Filter.byTypeAndId(MetricType.class, id).get());
        }

        @Override
        public MetricTypes.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), Filter.byType(MetricType.class).and(filters).get());
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

    public static final class Single extends Notifying.Relatable.Single<MetricTypes.Single>
            implements MetricTypes.Single {

        Single(MetricTypes.Single iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Metrics.Read metrics() {
            return wrapCall(ObservableMetrics.Read::new, iface.metrics(), Filter.relatedBy(defines).get());
        }

        @Override
        public MetricType entity() {
            return iface.entity();
        }
    }

    public static final class Multiple extends Notifying.Relatable.Multiple<MetricTypes.Multiple>
            implements MetricTypes.Multiple {

        Multiple(MetricTypes.Multiple iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Metrics.Read metrics() {
            return wrapCall(ObservableMetrics.Read::new, iface.metrics(), Filter.relatedBy(defines).get());
        }

        @Override
        public Set<MetricType> entities() {
            return iface.entities();
        }
    }
}
