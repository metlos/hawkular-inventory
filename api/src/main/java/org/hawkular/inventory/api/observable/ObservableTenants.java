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
import org.hawkular.inventory.api.Tenants;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.filters.Path;
import org.hawkular.inventory.api.filters.With;
import org.hawkular.inventory.api.model.Environment;
import org.hawkular.inventory.api.model.MetricType;
import org.hawkular.inventory.api.model.ResourceType;
import org.hawkular.inventory.api.model.Tenant;
import org.hawkular.inventory.api.observable.Notifying.NotificationContext;

import java.util.Set;

import static org.hawkular.inventory.api.Relationships.WellKnown.contains;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class ObservableTenants {

    private ObservableTenants() {

    }

    public static final class Read
            extends NotifyingWithDefaultObservables<Tenants.Read, Tenant> implements Tenants.Read {

        Read(Tenants.Read iface, NotificationContext notificationContext, Path path) {
            super(iface, Tenant.class, notificationContext, path);
        }

        @Override
        public ObservableTenants.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), With.id(id));
        }

        @Override
        public ObservableTenants.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), filters);
        }
    }

    public static final class ReadWrite
            extends NotifyingWithDefaultObservables<Tenants.ReadWrite, Tenant>
            implements Tenants.ReadWrite {

        ReadWrite(Tenants.ReadWrite tenants, NotificationContext notificationContext, Path path) {
            super(tenants, Tenant.class, notificationContext, path);
        }

        @Override
        public ObservableTenants.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), With.id(id));
        }

        @Override
        public ObservableTenants.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), filters);
        }

        @Override
        public ObservableTenants.Single create(String blueprint) throws EntityAlreadyExistsException {
            return wrapCallAndNotify(Single::new, () -> iface.create(blueprint), Action.CREATE,
                    (fs, s) -> new Contexts.EntityPath<>(Tenant.class, fs, s),
                    With.id(blueprint));
        }

        @Override
        public void update(Tenant tenant) throws EntityNotFoundException {
            Contexts.EntityPath<Tenant> ctx = new Contexts.EntityPath<>(Tenant.class, path, tenant);
            doAndNotify(iface::update, tenant, Action.UPDATE, ctx);
        }

        @Override
        public void delete(String id) throws EntityNotFoundException {
            Contexts.EntityPath<Tenant> ctx = new Contexts.EntityPath<>(Tenant.class, path, get(id).entity());
            doAndNotify(iface::delete, id, Action.DELETE, ctx);
        }
    }

    public static final class Single extends Notifying.Relatable.Single<Tenants.Single>
            implements Tenants.Single {

        Single(Tenants.Single tenants, NotificationContext notificationContext, Path path) {
            super(tenants, notificationContext, path);
        }

        @Override
        public ObservableResourceTypes.ReadWrite resourceTypes() {
            return wrapCall(ObservableResourceTypes.ReadWrite::new, iface.resourceTypes(),
                    Filter.relatedBy(contains).andType(ResourceType.class).get());
        }

        @Override
        public ObservableMetricTypes.ReadWrite metricTypes() {
            return wrapCall(ObservableMetricTypes.ReadWrite::new, iface.metricTypes(),
                    Filter.relatedBy(contains).andType(MetricType.class).get());
        }

        @Override
        public ObservableEnvironments.ReadWrite environments() {
            return wrapCall(ObservableEnvironments.ReadWrite::new, iface.environments(),
                    Filter.relatedBy(contains).andType(Environment.class).get());
        }

        @Override
        public Tenant entity() {
            return iface.entity();
        }
    }

    public static final class Multiple extends Notifying.Relatable.Multiple<Tenants.Multiple>
            implements Tenants.Multiple {

        Multiple(Tenants.Multiple tenants, NotificationContext notificationContext, Path path) {

            super(tenants, notificationContext, path);
        }

        @Override
        public ObservableResourceTypes.Read resourceTypes() {
            return wrapCall(ObservableResourceTypes.Read::new, iface.resourceTypes(), Filter.relatedBy(contains)
                    .andType(ResourceType.class).get());
        }

        @Override
        public ObservableMetricTypes.Read metricTypes() {
            return wrapCall(ObservableMetricTypes.Read::new, iface.metricTypes(), Filter.relatedBy(contains)
                    .andType(MetricType.class).get());
        }

        @Override
        public ObservableEnvironments.Read environments() {
            return wrapCall(ObservableEnvironments.Read::new, iface.environments(), Filter.relatedBy(contains)
                    .andType(Environment.class).get());
        }

        @Override
        public Set<Tenant> entities() {
            return iface.entities();
        }
    }
}
