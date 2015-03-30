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
import org.hawkular.inventory.api.ResourceTypes;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.filters.Path;
import org.hawkular.inventory.api.filters.With;
import org.hawkular.inventory.api.model.MetricType;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.model.ResourceType;
import org.hawkular.inventory.api.observable.Notifying.NotificationContext;

import java.util.Set;

import static org.hawkular.inventory.api.Relationships.WellKnown.defines;
import static org.hawkular.inventory.api.Relationships.WellKnown.owns;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class ObservableResourceTypes {

    private ObservableResourceTypes() {

    }

    public static final class Read
            extends NotifyingWithDefaultObservables<ResourceTypes.Read, ResourceType>
            implements ResourceTypes.Read {

        Read(ResourceTypes.Read iface, NotificationContext notificationContext, Path path) {
            super(iface, ResourceType.class, notificationContext, path);
        }

        @Override
        public ObservableResourceTypes.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), With.id(id));
        }

        @Override
        public ObservableResourceTypes.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), filters);
        }
    }

    public static final class ReadWrite
            extends NotifyingWithDefaultObservables<ResourceTypes.ReadWrite, ResourceType>
            implements ResourceTypes.ReadWrite {

        ReadWrite(ResourceTypes.ReadWrite iface, NotificationContext notificationContext, Path path) {
            super(iface, ResourceType.class, notificationContext, path);
        }

        @Override
        public ObservableResourceTypes.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), With.id(id));
        }

        @Override
        public ObservableResourceTypes.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), filters);
        }

        @Override
        public ObservableResourceTypes.Single create(ResourceType.Blueprint blueprint) throws EntityAlreadyExistsException {
            return wrapCallAndNotify(Single::new, () -> iface.create(blueprint), Action.CREATE,
                    (fs, s) -> new Contexts.EntityPath<>(ResourceType.class, fs, s),
                    Filter.byTypeAndId(ResourceType.class, blueprint.getId()).get());
        }

        @Override
        public void update(ResourceType resourceType) throws EntityNotFoundException {
            Contexts.EntityPath<ResourceType> ctx = new Contexts.EntityPath<>(ResourceType.class, path, resourceType);
            doAndNotify(iface::update, resourceType, Action.UPDATE, ctx);
        }

        @Override
        public void delete(String id) throws EntityNotFoundException {
            Contexts.EntityPath<ResourceType> ctx = new Contexts.EntityPath<>(ResourceType.class, path,
                    get(id).entity());
            doAndNotify(iface::delete, id, Action.UPDATE, ctx);
        }
    }

    public static final class Single extends Notifying.Relatable.Single<ResourceTypes.Single>
            implements ResourceTypes.Single {

        Single(ResourceTypes.Single iface, NotificationContext notificationContext, Path path) {
            super(iface, notificationContext, path);
        }

        @Override
        public ObservableResources.Read resources() {
            return wrapCall(ObservableResources.Read::new, iface.resources(), Filter.relatedBy(defines)
                    .andType(Resource.class).get());
        }

        @Override
        public ObservableMetricTypes.ReadRelate metricTypes() {
            return wrapCall(ObservableMetricTypes.ReadRelate::new, iface.metricTypes(), Filter.relatedBy(owns)
                    .andType(MetricType.class).get());
        }

        @Override
        public ResourceType entity() {
            return iface.entity();
        }
    }

    public static final class Multiple extends Notifying.Relatable.Multiple<ResourceTypes.Multiple>
        implements ResourceTypes.Multiple {

        Multiple(ResourceTypes.Multiple iface, NotificationContext notificationContext, Path path) {
            super(iface, notificationContext, path);
        }

        @Override
        public ObservableResources.Read resources() {
            return wrapCall(ObservableResources.Read::new, iface.resources(), Filter.relatedBy(defines)
                    .andType(Resource.class).get());
        }

        @Override
        public ObservableMetricTypes.Read metricTypes() {
            return wrapCall(ObservableMetricTypes.Read::new, iface.metricTypes(), Filter.relatedBy(owns)
                    .andType(MetricType.class).get());
        }

        @Override
        public Set<ResourceType> entities() {
            return iface.entities();
        }
    }
}
