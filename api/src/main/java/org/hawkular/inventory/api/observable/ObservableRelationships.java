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
import org.hawkular.inventory.api.Tenants;
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
        public Relationships.Multiple named(String name) {
            return iface.named(name);
        }

        @Override
        public Relationships.Multiple named(Relationships.WellKnown name) {
            return iface.named(name);
        }

        @Override
        public Relationships.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return iface.get(id);
        }

        @Override
        public Relationships.Multiple getAll(RelationFilter... filters) {
            return iface.getAll(filters);
        }

        @Override
        public Relationships.Single linkWith(String name, Entity targetOrSource)
                throws IllegalArgumentException {
            try {
                Relationships.Single ret = iface.linkWith(name, targetOrSource);
                notifyObservers(null, Action.CREATE, new Contexts.EntityPath<>(Relationship.class, path, ret.entity()));
                return ret;
            } catch (Exception e) {
                notifyObservers(e, Action.CREATE,
                        new Contexts.EntityPath<>(Relationship.class, path, (Relationship) null));
                throw e;
            }
        }

        @Override
        public Relationships.Single linkWith(Relationships.WellKnown name, Entity targetOrSource)
                throws IllegalArgumentException {
            return linkWith(name.name(), targetOrSource);
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

        Read(Relationships.Read iface, NotificationContext notificationContext, Path path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Relationships.Multiple named(String name) {
            return iface.named(name);
        }

        @Override
        public Relationships.Multiple named(Relationships.WellKnown name) {
            return iface.named(name);
        }

        @Override
        public Relationships.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return iface.get(id);
        }

        @Override
        public Relationships.Multiple getAll(RelationFilter... filters) {
            return iface.getAll(filters);
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
}
