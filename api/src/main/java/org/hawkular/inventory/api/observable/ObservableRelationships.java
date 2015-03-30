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
import org.hawkular.inventory.api.RelationNotFoundException;
import org.hawkular.inventory.api.Relationships;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.filters.Path;
import org.hawkular.inventory.api.filters.RelationFilter;
import org.hawkular.inventory.api.filters.RelationWith;
import org.hawkular.inventory.api.filters.With;
import org.hawkular.inventory.api.model.Entity;
import org.hawkular.inventory.api.model.Environment;
import org.hawkular.inventory.api.model.Feed;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.MetricType;
import org.hawkular.inventory.api.model.Relationship;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.model.ResourceType;
import org.hawkular.inventory.api.model.Tenant;

import java.util.Set;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class ObservableRelationships {
    private ObservableRelationships() {

    }

    public static final class ReadWrite
            extends Notifying<Relationships.ReadWrite> implements Relationships.ReadWrite {

        private final Relationships.Direction direction;

        ReadWrite(Relationships.ReadWrite iface, NotificationContext notificationContext,
                  Relationships.Direction direction, Path path) {
            super(iface, notificationContext, path);
            this.direction = direction;
        }

        @Override
        public ObservableRelationships.Multiple named(String name) {
            return wrapCall(Multiple::new, iface.named(name),
                    Filter.by(RelationWith.direction(direction), RelationWith.name(name)).get());
        }

        @Override
        public ObservableRelationships.Multiple named(Relationships.WellKnown name) {
            return wrapCall(Multiple::new, iface.named(name),
                    Filter.by(RelationWith.direction(direction), RelationWith.name(name.name())).get());
        }

        @Override
        public Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), Filter.by(RelationWith.id(id)).get());
        }

        @Override
        public ObservableRelationships.Multiple getAll(RelationFilter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters),
                    Filter.by(RelationWith.direction(direction)).and(filters).get());
        }

        @Override
        public ObservableRelationships.Single linkWith(String name, Entity targetOrSource)
                throws IllegalArgumentException {
            return wrapCallAndNotify(Single::new, () -> iface.linkWith(name, targetOrSource),
                    Action.CREATE,
                    (fs, s) -> new Contexts.EntityPath<>(Relationship.class, path, s));
        }

        @Override
        public ObservableRelationships.Single linkWith(Relationships.WellKnown name, Entity targetOrSource)
                throws IllegalArgumentException {
            return wrapCallAndNotify(Single::new, () -> iface.linkWith(name, targetOrSource),
                    Action.CREATE,
                    (fs, s) -> new Contexts.EntityPath<>(Relationship.class, path, s));
        }

        @Override
        public void update(Relationship relationship) throws RelationNotFoundException {
            doAndNotify(iface::update, relationship, Action.UPDATE,
                    new Contexts.EntityPath<>(Relationship.class, path, relationship));
        }

        @Override
        public void delete(String id) throws RelationNotFoundException {
            doAndNotify(iface::delete, id, Action.DELETE,
                    new Contexts.EntityPath<>(Relationship.class,
                            path.extend(RelationWith.direction(direction), RelationWith.id(id)), get(id).entity()));
        }

        public Observable<Contexts.EntityPath<Relationship>> onCreate() {
            return new ObservableImpl<>(notificationContext, path, Action.CREATE);
        }

        public Observable<Contexts.EntityPath<Relationship>> onUpdate() {
            return new ObservableImpl<>(notificationContext, path, Action.UPDATE);
        }

        public Observable<Contexts.EntityPath<Relationship>> onDelete() {
            return new ObservableImpl<>(notificationContext, path, Action.DELETE);
        }
    }

    public static final class Read extends Notifying<Relationships.Read> implements Relationships.Read {

        private final Relationships.Direction direction;

        Read(Relationships.Read iface, NotificationContext notificationContext,
                  Relationships.Direction direction, Path path) {
            super(iface, notificationContext, path);
            this.direction = direction;
        }

        @Override
        public ObservableRelationships.Multiple named(String name) {
            return wrapCall(Multiple::new, iface.named(name),
                    Filter.by(RelationWith.direction(direction), RelationWith.name(name)).get());
        }

        @Override
        public ObservableRelationships.Multiple named(Relationships.WellKnown name) {
            return wrapCall(Multiple::new, iface.named(name),
                    Filter.by(RelationWith.direction(direction), RelationWith.name(name.name())).get());
        }

        @Override
        public ObservableRelationships.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id),
                    Filter.by(RelationWith.direction(direction), RelationWith.id(id)).get());
        }

        @Override
        public ObservableRelationships.Multiple getAll(RelationFilter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters),
                    Filter.by(RelationWith.direction(direction)).and(filters).get());
        }
    }

    public static final class Single extends Notifying<Relationships.Single> implements Relationships.Single {

        Single(Relationships.Single iface, NotificationContext notificationContext, Path path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Relationship entity() {
            return iface.entity();
        }
    }

    public static final class Multiple extends Notifying<Relationships.Multiple> implements Relationships.Multiple {
        Multiple(Relationships.Multiple iface, NotificationContext notificationContext, Path path) {
            super(iface, notificationContext, path);
        }

        @Override
        public ObservableTenants.Read tenants() {
            return wrapCall(ObservableTenants.Read::new, iface.tenants(), With.type(Tenant.class));
        }

        @Override
        public ObservableEnvironments.Read environments() {
            return wrapCall(ObservableEnvironments.Read::new, iface.environments(), With.type(Environment.class));
        }

        @Override
        public ObservableFeeds.Read feeds() {
            return wrapCall(ObservableFeeds.Read::new, iface.feeds(), With.type(Feed.class));
        }

        @Override
        public ObservableMetricTypes.Read metricTypes() {
            return wrapCall(ObservableMetricTypes.Read::new, iface.metricTypes(), With.type(MetricType.class));
        }

        @Override
        public ObservableMetrics.Read metrics() {
            return wrapCall(ObservableMetrics.Read::new, iface.metrics(), With.type(Metric.class));
        }

        @Override
        public ObservableResources.Read resources() {
            return wrapCall(ObservableResources.Read::new, iface.resources(), With.type(Resource.class));
        }

        @Override
        public ObservableResourceTypes.Read resourceTypes() {
            return wrapCall(ObservableResourceTypes.Read::new, iface.resourceTypes(), With.type(ResourceType.class));
        }

        @Override
        public Set<Relationship> entities() {
            return iface.entities();
        }
    }
}
