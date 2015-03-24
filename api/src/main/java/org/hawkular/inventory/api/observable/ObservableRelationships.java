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

import org.hawkular.inventory.api.EntityNotFoundException;
import org.hawkular.inventory.api.Environments;
import org.hawkular.inventory.api.Feeds;
import org.hawkular.inventory.api.MetricTypes;
import org.hawkular.inventory.api.Metrics;
import org.hawkular.inventory.api.RelationNotFoundException;
import org.hawkular.inventory.api.Relationships;
import org.hawkular.inventory.api.ResourceTypes;
import org.hawkular.inventory.api.Resources;
import org.hawkular.inventory.api.Tenants;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.filters.RelationFilter;
import org.hawkular.inventory.api.filters.RelationWith;
import org.hawkular.inventory.api.model.Entity;
import org.hawkular.inventory.api.model.Relationship;

import java.util.Set;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class ObservableRelationships {
    private ObservableRelationships() {

    }

    public static final class ReadWrite extends Notifying<Relationships.ReadWrite>
            implements Relationships.ReadWrite {

        private final Relationships.Direction direction;

        ReadWrite(Relationships.ReadWrite iface, ObserverNotificationStrategy notificationStrategy,
                  Relationships.Direction direction, Filter... path) {
            super(iface, notificationStrategy, path);
            this.direction = direction;
        }

        @Override
        public Multiple named(String name) {
            return wrapCall(Multiple::new, iface.named(name),
                    Filter.by(RelationWith.direction(direction), RelationWith.name(name)).get());
        }

        @Override
        public Multiple named(Relationships.WellKnown name) {
            return wrapCall(Multiple::new, iface.named(name),
                    Filter.by(RelationWith.direction(direction), RelationWith.name(name.name())).get());
        }

        @Override
        public Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), Filter.by(RelationWith.id(id)).get());
        }

        @Override
        public Relationships.Multiple getAll(RelationFilter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters),
                    Filter.by(RelationWith.direction(direction)).and(filters).get());
        }

        @Override
        public Relationships.Single linkWith(String name, Entity targetOrSource) throws IllegalArgumentException {
            return wrapCallAndNotify(Single::new, iface.linkWith(name, targetOrSource), new Actions.LinkedAction(),
                    (fs) -> new Actions.LinkAction.RelationshipEnds(name, direction, path, targetOrSource), path);
        }

        @Override
        public Relationships.Single linkWith(Relationships.WellKnown name, Entity targetOrSource) throws IllegalArgumentException {
            return wrapCallAndNotify(Single::new, iface.linkWith(name, targetOrSource), new Actions.LinkedAction(),
                    (fs) -> new Actions.LinkAction.RelationshipEnds(name.name(), direction, path, targetOrSource),
                        path);
        }

        @Override
        public void update(Relationship relationship) throws RelationNotFoundException {
            doAndNotify(iface::update, relationship, new Actions.UpdateAction<>(),
                    new Actions.PathContext<>(Relationship.class, path));
        }

        @Override
        public void delete(String id) throws RelationNotFoundException {
            doAndNotify(iface::delete, id, new Actions.DeleteAction<>(),
                    new Actions.PathContext<>(Relationship.class,
                            Filter.by(RelationWith.direction(direction), RelationWith.id(id)).get()));
        }
    }

    public static final class Read extends Notifying<Relationships.Read> implements Relationships.Read {

        private final Relationships.Direction direction;

        Read(Relationships.Read iface, ObserverNotificationStrategy notificationStrategy,
                  Relationships.Direction direction, Filter[] path) {
            super(iface, notificationStrategy, path);
            this.direction = direction;
        }

        @Override
        public Relationships.Multiple named(String name) {
            return wrapCall(Multiple::new, iface.named(name),
                    Filter.by(RelationWith.direction(direction), RelationWith.name(name)).get());
        }

        @Override
        public Relationships.Multiple named(Relationships.WellKnown name) {
            return wrapCall(Multiple::new, iface.named(name),
                    Filter.by(RelationWith.direction(direction), RelationWith.name(name.name())).get());
        }

        @Override
        public Relationships.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id),
                    Filter.by(RelationWith.direction(direction), RelationWith.id(id)).get());
        }

        @Override
        public Relationships.Multiple getAll(RelationFilter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters),
                    Filter.by(RelationWith.direction(direction)).and(filters).get());
        }
    }

    public static final class Single extends Notifying<Relationships.Single> implements Relationships.Single {

        Single(Relationships.Single iface, ObserverNotificationStrategy notificationStrategy, Filter[] path) {
            super(iface, notificationStrategy, path);
        }

        @Override
        public Relationship entity() {
            return iface.entity();
        }
    }

    public static final class Multiple extends Notifying<Relationships.Multiple> implements Relationships.Multiple {
        Multiple(Relationships.Multiple iface, ObserverNotificationStrategy notificationStrategy, Filter... path) {
            super(iface, notificationStrategy, path);
        }

        @Override
        public Tenants.Read tenants() {
            return wrapCall(ObservableTenants.Read::new, iface.tenants());
        }

        @Override
        public Environments.Read environments() {
            return wrapCall(ObservableEnvironments.Read::new, iface.environments());
        }

        @Override
        public Feeds.Read feeds() {
            return wrapCall(ObservableFeeds.Read::new, iface.feeds());
        }

        @Override
        public MetricTypes.Read metricTypes() {
            return wrapCall(ObservableMetricTypes.Read::new, iface.metricTypes());
        }

        @Override
        public Metrics.Read metrics() {
            return wrapCall(ObservableMetrics.Read::new, iface.metrics());
        }

        @Override
        public Resources.Read resources() {
            return wrapCall(ObservableResources.Read::new, iface.resources());
        }

        @Override
        public ResourceTypes.Read resourceTypes() {
            return wrapCall(ObservableResourceTypes.Read::new, iface.resourceTypes());
        }

        @Override
        public Set<Relationship> entities() {
            return iface.entities();
        }
    }
}
