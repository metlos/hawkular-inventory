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

import org.hawkular.inventory.api.Relationships;
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.filters.Path;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
* @author Lukas Krejci
* @since 0.0.1
*/
public abstract class Notifying<T> {
    protected final T iface;
    protected final NotificationContext notificationContext;
    protected final Path path;

    Notifying(T iface, NotificationContext context, Path path) {
        this.iface = iface;
        this.notificationContext = context;
        this.path = path;
    }

    protected final <C> void notifyObservers(Throwable failure, Action action, C actionContext) {
        Set<Observer<C>> observers = notificationContext.storage.getObservers(action, path);
        observers.forEach((o) -> {
            if (failure == null) {
                notificationContext.strategy.notifySuccess(o, action, actionContext);
            } else {
                notificationContext.strategy.notifyFailure(o, failure, action, actionContext);
            }
        });
    }

    protected <I, R> R wrapCall(TriFunction<I, NotificationContext, Path, R> ctor, I iface,
                                Filter... pathExtension) {
        return ctor.apply(iface, notificationContext, path.extend(pathExtension));
    }

    protected <I, R, C> R wrapCallAndNotify(TriFunction<I, NotificationContext, Path, R> ctor,
                                            Supplier<I> ifaceSupplier, Action action,
                                            BiFunction<Path, Optional<R>, C> contextProducer, Filter... pathExtension) {
        Path newPath = path.extend(pathExtension);
        try {
            I iface = ifaceSupplier.get();
            R ret = wrapCall(ctor, iface, pathExtension);
            notifyObservers(null, action, contextProducer.apply(newPath, Optional.ofNullable(ret)));
            return ret;
        } catch (Throwable t) {
            notifyObservers(t, action, contextProducer.apply(newPath, Optional.empty()));
            throw t;
        }
    }

    protected <P, C> void doAndNotify(Consumer<P> f, P param, Action action, C context) {
        try {
            f.accept(param);
            notifyObservers(null, action, context);
        } catch (Throwable t) {
            notifyObservers(t, action, context);
            throw t;
        }
    }

    protected <P1, P2, C> void doAndNotify(BiConsumer<P1, P2> f, P1 p1, P2 p2, Action action, C context) {
        try {
            f.accept(p1, p2);
            notifyObservers(null, action, context);
        } catch (Throwable t) {
            notifyObservers(t, action, context);
            throw t;
        }
    }


    protected interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
    
    protected interface TetraFunction<T, U, V, W, R> {
        R apply(T t, U u, V v, W w);
    }

    public static class Relatable<T> {

        protected final T iface;
        protected final NotificationContext notificationContext;
        protected final Path path;

        private Relatable(T iface, NotificationContext notificationContext, Path path) {
            this.iface = iface;

            this.notificationContext = notificationContext;
            this.path = path;
        }

        protected <I, R> R wrapCall(TriFunction<I, NotificationContext, Path, R> ctor, I iface,
                                    Filter... pathExtension) {
            return ctor.apply(iface, notificationContext, path.extend(pathExtension));
        }

        protected <I, P, R> R wrapCall(TetraFunction<I, NotificationContext, P, Path, R> ctor, I iface,
                                       P param, Filter... pathExtension) {
            return ctor.apply(iface, notificationContext, param, path.extend(pathExtension));
        }

        public static abstract class Single<T extends org.hawkular.inventory.api.Relatable<Relationships.ReadWrite>>
                extends Relatable<T> {

            protected Single(T iface, NotificationContext notificationContext, Path path) {
                super(iface, notificationContext, path);
            }

            public ObservableRelationships.ReadWrite relationships() {
                return relationships(Relationships.Direction.outgoing);
            }

            public ObservableRelationships.ReadWrite relationships(Relationships.Direction direction) {
                return wrapCall(ObservableRelationships.ReadWrite::new, iface.relationships(direction), direction);
            }
        }

        public static abstract class Multiple<T extends org.hawkular.inventory.api.Relatable<Relationships.Read>>
                extends Relatable<T> {

            protected Multiple(T iface, NotificationContext notificationContext, Path path) {
                super(iface, notificationContext, path);
            }

            public ObservableRelationships.Read relationships() {
                return relationships(Relationships.Direction.outgoing);
            }

            public ObservableRelationships.Read relationships(Relationships.Direction direction) {
                return wrapCall(ObservableRelationships.Read::new, iface.relationships(direction));
            }
        }
    }

    protected static class NotificationContext {
        public final ObserverNotificationStrategy strategy;
        public final SharedObserverStorage storage;

        public NotificationContext(SharedObserverStorage storage, ObserverNotificationStrategy strategy) {
            this.storage = storage;
            this.strategy = strategy;
        }
    }
}
