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

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
* @author Lukas Krejci
* @since 0.0.1
*/
class Notifying<T> extends ObservableBase {
    protected final T iface;

    protected Notifying(T iface, NotificationContext context, Filter[] path) {
        super(context, path);
        this.iface = iface;
    }

    protected <I, R> R wrapCall(TriFunction<I, NotificationContext, Filter[], R> ctor, I iface,
                                Filter... pathExtension) {
        return ctor.apply(iface, notificationContext, Filter.by(path).and(pathExtension).get());
    }

    protected <I, P, R> R wrapCall(TetraFunction<I, NotificationContext, P, Filter[], R> ctor, I iface,
                                   P param, Filter... pathExtension) {
        return ctor.apply(iface, notificationContext, param, Filter.by(path).and(pathExtension).get());
    }
    
    protected <I, R, C> R wrapCallAndNotify(TriFunction<I, NotificationContext, Filter[], R> ctor, I iface,
                                         Action<C> action, Function<Filter[], C> contextProducer,
                                         Filter... pathExtension) {
        Filter[] newPath = Filter.by(path).and(pathExtension).get();
        try {
            R ret = wrapCall(ctor, iface, newPath);
            notifyObservers(null, action, contextProducer.apply(newPath));
            return ret;
        } catch (Throwable t) {
            notifyObservers(t, action, contextProducer.apply(newPath));
            throw t;
        }
    }

    protected <P, C> void doAndNotify(Consumer<P> f, P param, Action<C> action, C context) {
        try {
            f.accept(param);
            notifyObservers(null, action, context);
        } catch (Throwable t) {
            notifyObservers(t, action, context);
            throw t;
        }
    }

    protected <P1, P2, C> void doAndNotify(BiConsumer<P1, P2> f, P1 p1, P2 p2, Action<C> action, C context) {
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

    static final class Relatable {

        private Relatable() {

        }

        public static abstract class Single<T extends org.hawkular.inventory.api.Relatable<Relationships.ReadWrite>>
                extends Notifying<T> {

            protected Single(T iface, NotificationContext notificationContext, Filter[] path) {
                super(iface, notificationContext, path);
            }

            public Relationships.ReadWrite relationships() {
                return relationships(Relationships.Direction.outgoing);
            }

            public Relationships.ReadWrite relationships(Relationships.Direction direction) {
                return wrapCall(ObservableRelationships.ReadWrite::new, iface.relationships(direction), direction,
                        Filter.all());
            }
        }

        public static abstract class Multiple<T extends org.hawkular.inventory.api.Relatable<Relationships.Read>>
                extends Notifying<T> {

            protected Multiple(T iface, NotificationContext notificationContext, Filter[] path) {
                super(iface, notificationContext, path);
            }

            public Relationships.Read relationships() {
                return relationships(Relationships.Direction.outgoing);
            }

            public Relationships.Read relationships(Relationships.Direction direction) {
                return wrapCall(ObservableRelationships.Read::new, iface.relationships(direction), direction);
            }
        }
    }
}
