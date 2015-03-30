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
import org.hawkular.inventory.api.RelationNotFoundException;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.filters.Path;
import org.hawkular.inventory.api.filters.With;
import org.hawkular.inventory.api.model.Environment;
import org.hawkular.inventory.api.model.Feed;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.observable.Notifying.NotificationContext;

import java.util.Set;

import static org.hawkular.inventory.api.Relationships.WellKnown.contains;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class ObservableEnvironments {

    private ObservableEnvironments() {

    }

    public static final class ReadWrite
            extends NotifyingWithDefaultObservables<Environments.ReadWrite, Environment>
            implements Environments.ReadWrite {

        ReadWrite(Environments.ReadWrite iface, NotificationContext notificationContext, Path path) {
            super(iface, Environment.class, notificationContext, path);
        }

        @Override
        public void copy(String sourceEnvironmentId, String targetEnvironmentId) {
            Contexts.EnvironmentCopy ctx = new Contexts.EnvironmentCopy(
                    path.extend(Filter.byId(sourceEnvironmentId)), targetEnvironmentId);

            doAndNotify(iface::copy, sourceEnvironmentId, targetEnvironmentId, Action.COPY, ctx);
        }

        @Override
        public ObservableEnvironments.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), With.id(id));
        }

        @Override
        public ObservableEnvironments.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), filters);
        }

        @Override
        public ObservableEnvironments.Single create(String blueprint) throws EntityAlreadyExistsException {
            return wrapCallAndNotify(Single::new, () -> iface.create(blueprint), Action.CREATE,
                    (fs, s) -> new Contexts.EntityPath<>(Environment.class, fs, s), With.id(blueprint));
        }

        @Override
        public void update(Environment environment) throws EntityNotFoundException {
            Contexts.EntityPath<Environment> ctx = new Contexts.EntityPath<>(Environment.class, path, environment);
            doAndNotify(iface::update, environment, Action.UPDATE, ctx);
        }

        @Override
        public void delete(String id) throws EntityNotFoundException {
            Contexts.EntityPath<Environment> ctx = new Contexts.EntityPath<>(Environment.class, path, get(id).entity());
            doAndNotify(iface::delete, id, Action.DELETE, ctx);
        }

        public Observable<Contexts.EntityPath<Environment>> onCopy() {
            return new ObservableImpl<>(notificationContext, path, Action.COPY);
        }
    }

    public static final class Read
            extends NotifyingWithDefaultObservables<Environments.Read, Environment> implements Environments.Read {

        Read(Environments.Read iface, NotificationContext notificationContext, Path path) {
            super(iface, Environment.class, notificationContext, path);
        }

        @Override
        public ObservableEnvironments.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), With.id(id));
        }

        @Override
        public ObservableEnvironments.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), filters);
        }

        public Observable<Contexts.EntityPath<Environment>> onCopy() {
            return new ObservableImpl<>(notificationContext, path, Action.COPY);
        }
    }

    public static final class Single extends Notifying.Relatable.Single<Environments.Single>
            implements Environments.Single {

        Single(Environments.Single iface, NotificationContext notificationContext, Path path) {
            super(iface, notificationContext, path);
        }

        @Override
        public ObservableFeeds.ReadAndRegister feeds() {
            return wrapCall(ObservableFeeds.ReadAndRegister::new, iface.feeds(), Filter.relatedBy(contains)
                    .andType(Feed.class).get());
        }

        @Override
        public ObservableResources.ReadWrite resources() {
            return wrapCall(ObservableResources.ReadWrite::new, iface.resources(), Filter.relatedBy(contains)
                    .andType(Resource.class).get());
        }

        @Override
        public ObservableMetrics.ReadWrite metrics() {
            return wrapCall(ObservableMetrics.ReadWrite::new, iface.metrics(), Filter.relatedBy(contains)
                    .andType(Metric.class).get());
        }

        @Override
        public Environment entity() {
            return iface.entity();
        }
    }

    public static final class Multiple extends Notifying.Relatable.Multiple<Environments.Multiple>
            implements Environments.Multiple {

        Multiple(Environments.Multiple iface, NotificationContext notificationContext, Path path) {
            super(iface, notificationContext, path);
        }

        @Override
        public ObservableFeeds.Read feeds() {
            return wrapCall(ObservableFeeds.Read::new, iface.feeds(), Filter.relatedBy(contains)
                    .andType(Feed.class).get());
        }

        @Override
        public ObservableResources.Read resources() {
            return wrapCall(ObservableResources.Read::new, iface.resources(), Filter.relatedBy(contains)
                    .andType(Resource.class).get());
        }

        @Override
        public ObservableMetrics.Read metrics() {
            return wrapCall(ObservableMetrics.Read::new, iface.metrics(), Filter.relatedBy(contains)
                    .andType(Metric.class).get());
        }

        @Override
        public Set<Environment> entities() {
            return iface.entities();
        }
    }
}
