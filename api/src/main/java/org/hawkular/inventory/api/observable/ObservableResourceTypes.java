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
import org.hawkular.inventory.api.ResourceTypes;
import org.hawkular.inventory.api.Resources;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.model.ResourceType;

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

    public static final class Read extends Notifying<ResourceTypes.Read> implements ResourceTypes.Read {

        Read(ResourceTypes.Read iface, ObserverNotificationStrategy notificationStrategy, Filter[] path) {
            super(iface, notificationStrategy, path);
        }

        @Override
        public ResourceTypes.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), Filter.byTypeAndId(ResourceType.class, id).get());
        }

        @Override
        public ResourceTypes.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), Filter.byType(ResourceType.class).and(filters).get());
        }
    }

    public static final class ReadWrite extends Notifying<ResourceTypes.ReadWrite> implements ResourceTypes.ReadWrite {

        ReadWrite(ResourceTypes.ReadWrite iface, ObserverNotificationStrategy notificationStrategy, Filter[] path) {
            super(iface, notificationStrategy, path);
        }

        @Override
        public ResourceTypes.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), Filter.byTypeAndId(ResourceType.class, id).get());
        }

        @Override
        public ResourceTypes.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), Filter.byType(ResourceType.class).and(filters).get());
        }

        @Override
        public ResourceTypes.Single create(ResourceType.Blueprint blueprint) throws EntityAlreadyExistsException {
            return wrapCallAndNotify(Single::new, iface.create(blueprint), new Actions.CreateAction<>(),
                    (fs) -> new Actions.PathContext<>(ResourceType.class, fs),
                    Filter.byTypeAndId(ResourceType.class, blueprint.getId()).get());
        }

        @Override
        public void update(ResourceType resourceType) throws EntityNotFoundException {
            Actions.PathContext<ResourceType> ctx = new Actions.PathContext<>(ResourceType.class, path);
            doAndNotify(iface::update, resourceType, new Actions.UpdateAction<>(), ctx);
        }

        @Override
        public void delete(String id) throws EntityNotFoundException {
            Actions.PathContext<ResourceType> ctx = new Actions.PathContext<>(ResourceType.class, path);
            doAndNotify(iface::delete, id, new Actions.UpdateAction<>(), ctx);
        }
    }

    public static final class Single extends Notifying.Relatable.Single<ResourceTypes.Single>
            implements ResourceTypes.Single {

        Single(ResourceTypes.Single iface, ObserverNotificationStrategy notificationStrategy, Filter[] path) {
            super(iface, notificationStrategy, path);
        }

        @Override
        public Resources.Read resources() {
            return wrapCall(ObservableResources.Read::new, iface.resources(), Filter.relatedBy(defines).get());
        }

        @Override
        public MetricTypes.ReadRelate metricTypes() {
            return wrapCall(ObservableMetricTypes.ReadRelate::new, iface.metricTypes(), Filter.relatedBy(owns).get());
        }

        @Override
        public ResourceType entity() {
            return iface.entity();
        }
    }

    public static final class Multiple extends Notifying.Relatable.Multiple<ResourceTypes.Multiple>
        implements ResourceTypes.Multiple {

        Multiple(ResourceTypes.Multiple iface, ObserverNotificationStrategy notificationStrategy, Filter[] path) {
            super(iface, notificationStrategy, path);
        }

        @Override
        public Resources.Read resources() {
            return wrapCall(ObservableResources.Read::new, iface.resources(), Filter.relatedBy(defines).get());
        }

        @Override
        public MetricTypes.Read metricTypes() {
            return wrapCall(ObservableMetricTypes.Read::new, iface.metricTypes(), Filter.relatedBy(owns).get());
        }

        @Override
        public Set<ResourceType> entities() {
            return iface.entities();
        }
    }
}
