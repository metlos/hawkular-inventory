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
import org.hawkular.inventory.api.Environments;
import org.hawkular.inventory.api.MetricTypes;
import org.hawkular.inventory.api.RelationNotFoundException;
import org.hawkular.inventory.api.ResourceTypes;
import org.hawkular.inventory.api.Tenants;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.model.Tenant;

import java.util.Set;

import static org.hawkular.inventory.api.Relationships.WellKnown.contains;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class ObservableTenants {

    private ObservableTenants() {

    }

    public static final class Read extends Notifying<Tenants.Read> implements Tenants.Read {

        Read(Tenants.Read iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Tenants.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(TenantsSingle::new, iface.get(id), Filter.byTypeAndId(Tenant.class, id).get());
        }

        @Override
        public Tenants.Multiple getAll(Filter... filters) {
            return wrapCall(TenantsMultiple::new, iface.getAll(filters),
                    Filter.byType(Tenant.class).and(filters).get());
        }
    }

    public static final class ReadWrite extends Notifying<Tenants.ReadWrite> implements Tenants.ReadWrite {

        ReadWrite(Tenants.ReadWrite tenants, NotificationContext notificationContext,
                          Filter[] path) {
            super(tenants, notificationContext, path);
        }

        @Override
        public Tenants.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(TenantsSingle::new, iface.get(id), Filter.byTypeAndId(Tenant.class, id).get());
        }

        @Override
        public Tenants.Multiple getAll(Filter... filters) {
            return wrapCall(TenantsMultiple::new, iface.getAll(filters),
                    Filter.byType(Tenant.class).and(filters).get());
        }

        @Override
        public Tenants.Single create(String s) throws EntityAlreadyExistsException {
            return wrapCallAndNotify(TenantsSingle::new, iface.create(s), new Actions.CreateAction<>(),
                    (fs) -> new Actions.PathContext<>(Tenant.class, fs), Filter.byTypeAndId(Tenant.class, s).get());
        }

        @Override
        public void update(Tenant tenant) throws EntityNotFoundException {
            Actions.PathContext<Tenant> ctx = new Actions.PathContext<>(Tenant.class, path);
            doAndNotify(iface::update, tenant, new Actions.UpdateAction<>(), ctx);
        }

        @Override
        public void delete(String id) throws EntityNotFoundException {
            Actions.PathContext<Tenant> ctx = new Actions.PathContext<>(Tenant.class, path);
            doAndNotify(iface::delete, id, new Actions.DeleteAction<>(), ctx);
        }
    }

    public static final class TenantsSingle extends Notifying.Relatable.Single<Tenants.Single>
            implements Tenants.Single {

        TenantsSingle(Tenants.Single tenants, NotificationContext notificationContext, Filter[] path) {
            super(tenants, notificationContext, path);
        }

        @Override
        public ResourceTypes.ReadWrite resourceTypes() {
            return wrapCall(ObservableResourceTypes.ReadWrite::new, iface.resourceTypes(),
                    Filter.relatedBy(contains).get());
        }

        @Override
        public MetricTypes.ReadWrite metricTypes() {
            return wrapCall(ObservableMetricTypes.ReadWrite::new, iface.metricTypes(),
                    Filter.relatedBy(contains).get());
        }

        @Override
        public Environments.ReadWrite environments() {
            return wrapCall(ObservableEnvironments.ReadWrite::new, iface.environments(),
                    Filter.relatedBy(contains).get());
        }

        @Override
        public Tenant entity() {
            return iface.entity();
        }
    }

    public static final class TenantsMultiple extends Notifying.Relatable.Multiple<Tenants.Multiple>
            implements Tenants.Multiple {

        TenantsMultiple(Tenants.Multiple tenants, NotificationContext notificationContext,
            Filter[] path) {

            super(tenants, notificationContext, path);
        }

        @Override
        public ResourceTypes.Read resourceTypes() {
            return wrapCall(ObservableResourceTypes.Read::new, iface.resourceTypes(), Filter.relatedBy(contains).get());
        }

        @Override
        public MetricTypes.Read metricTypes() {
            return wrapCall(ObservableMetricTypes.Read::new, iface.metricTypes(), Filter.relatedBy(contains).get());
        }

        @Override
        public Environments.Read environments() {
            return wrapCall(ObservableEnvironments.Read::new, iface.environments(), Filter.relatedBy(contains).get());
        }

        @Override
        public Set<Tenant> entities() {
            return iface.entities();
        }
    }
}
