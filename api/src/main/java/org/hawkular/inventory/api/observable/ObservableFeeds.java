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
import org.hawkular.inventory.api.Feeds;
import org.hawkular.inventory.api.RelationNotFoundException;
import org.hawkular.inventory.api.Relationships;
import org.hawkular.inventory.api.Resources;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.filters.With;
import org.hawkular.inventory.api.model.Feed;

import java.util.Set;

import static org.hawkular.inventory.api.Relationships.WellKnown.owns;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class ObservableFeeds {
    private ObservableFeeds() {

    }

    public static final class ReadAndRegister extends Notifying<Feeds.ReadAndRegister>
            implements Feeds.ReadAndRegister {


        ReadAndRegister(Feeds.ReadAndRegister iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Feeds.Single register(String proposedId) {
            return wrapCallAndNotify(Single::new, iface.register(proposedId), new Actions.RegisterAction(),
                    (fs) -> new Actions.PathContext<>(Feed.class, fs),
                    Filter.byTypeAndId(Feed.class, proposedId).get());
        }

        @Override
        public Feeds.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), Filter.byTypeAndId(Feed.class, id).get());
        }

        @Override
        public Feeds.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters), Filter.byType(Feed.class).and(filters).get());
        }
    }

    public static final class Read extends Notifying<Feeds.Read> implements Feeds.Read {
        Read(Feeds.Read iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Feeds.Single get(String id) throws EntityNotFoundException, RelationNotFoundException {
            return wrapCall(Single::new, iface.get(id), Filter.by(With.type(Feed.class), With.id(id)).get());
        }

        @Override
        public Feeds.Multiple getAll(Filter... filters) {
            return wrapCall(Multiple::new, iface.getAll(filters),
                    Filter.by(With.type(Feed.class)).and(filters).get());
        }
    }

    public static final class Single extends Notifying.Relatable.Single<Feeds.Single> implements Feeds.Single {

        Single(Feeds.Single iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Resources.Read resources() {
            return wrapCall(ObservableResources.Read::new, iface.resources(), Filter.relatedBy(owns).get());
        }

        @Override
        public Feed entity() {
            return iface.entity();
        }
    }

    public static final class Multiple extends Notifying.Relatable.Multiple<Feeds.Multiple> implements Feeds.Multiple {

        Multiple(Feeds.Multiple iface, NotificationContext notificationContext, Filter[] path) {
            super(iface, notificationContext, path);
        }

        @Override
        public Resources.Read resources() {
            return wrapCall(ObservableResources.Read::new, iface.resources(), Filter.relatedBy(owns).get());
        }

        @Override
        public Set<Feed> entities() {
            return iface.entities();
        }
    }
}
