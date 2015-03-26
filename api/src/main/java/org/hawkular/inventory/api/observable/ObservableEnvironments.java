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
import org.hawkular.inventory.api.Feeds;
import org.hawkular.inventory.api.Metrics;
import org.hawkular.inventory.api.RelationNotFoundException;
import org.hawkular.inventory.api.Resources;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.model.Environment;

import java.util.Set;

import static org.hawkular.inventory.api.Relationships.WellKnown.contains;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class ObservableEnvironments {

    private ObservableEnvironments() {

    }

    public static final class ReadWrite extends Notifying<Environments.ReadWrite> implements Environments.ReadWrite {

        ReadWrite(Environments.ReadWrite iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public void copy(String sourceEnvironmentId, String targetEnvironmentId) {
            Actions.CopyAction.Environments ctx = new Actions.CopyAction.Environments(
                    Filter.by(path).andType(Environment.class).andId(sourceEnvironmentId).get(), targetEnvironmentId);

            doAndNotify(iface::copy, sourceEnvironmentId, targetEnvironmentId, new Actions.CopyAction(),ctx);
        }

        @Override
        public Environments.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), Filter.byTypeAndId(Environment.class, id).get());
        }

        @Override
        public Environments.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), Filter.byType(Environment.class).and(filters).get());
        }

        @Override
        public Environments.Single create(String s) throws EntityAlreadyExistsException {
            return wrapCallAndNotify(Single::new, iface.create(s), new Actions.CreateAction<>(),
                    (fs) -> new Actions.PathContext<>(Environment.class, fs),
                    Filter.byTypeAndId(Environment.class, s).get());
        }

        @Override
        public void update(Environment environment) throws EntityNotFoundException {
            Actions.PathContext<Environment> ctx = new Actions.PathContext<>(Environment.class, path);
            doAndNotify(iface::update, environment, new Actions.UpdateAction<>(), ctx);
        }

        @Override
        public void delete(String id) throws EntityNotFoundException {
            Actions.PathContext<Environment> ctx = new Actions.PathContext<>(Environment.class, path);
            doAndNotify(iface::delete, id, new Actions.DeleteAction<>(), ctx);
        }
    }

    public static final class Read extends Notifying<Environments.Read> implements Environments.Read {

        Read(Environments.Read iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Environments.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id),
                    Filter.byTypeAndId(Environment.class, id).get());
        }

        @Override
        public Environments.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters),
                    Filter.byType(Environment.class).and(filters).get());
        }
    }

    public static final class Single extends Notifying.Relatable.Single<Environments.Single>
            implements Environments.Single {

        Single(Environments.Single iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Feeds.ReadAndRegister feeds() {
            return wrapCall(ObservableFeeds.ReadAndRegister::new, iface.feeds(), Filter.relatedBy(contains).get());
        }

        @Override
        public Resources.ReadWrite resources() {
            return wrapCall(ObservableResources.ReadWrite::new, iface.resources(), Filter.relatedBy(contains).get());
        }

        @Override
        public Metrics.ReadWrite metrics() {
            return wrapCall(ObservableMetrics.ReadWrite::new, iface.metrics(), Filter.relatedBy(contains).get());
        }

        @Override
        public Environment entity() {
            return iface.entity();
        }
    }

    public static final class Multiple extends Notifying.Relatable.Multiple<Environments.Multiple>
            implements Environments.Multiple {

        Multiple(Environments.Multiple iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Feeds.Read feeds() {
            return wrapCall(ObservableFeeds.Read::new, iface.feeds(), Filter.relatedBy(contains).get());
        }

        @Override
        public Resources.Read resources() {
            return wrapCall(ObservableResources.Read::new, iface.resources(), Filter.relatedBy(contains).get());
        }

        @Override
        public Metrics.Read metrics() {
            return wrapCall(ObservableMetrics.Read::new, iface.metrics(), Filter.relatedBy(contains).get());
        }

        @Override
        public Set<Environment> entities() {
            return iface.entities();
        }
    }
}
