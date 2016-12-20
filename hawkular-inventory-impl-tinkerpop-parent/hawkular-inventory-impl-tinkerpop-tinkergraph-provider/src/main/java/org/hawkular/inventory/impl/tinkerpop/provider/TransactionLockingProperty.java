/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.inventory.impl.tinkerpop.provider;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedProperty;

/**
 * @author Lukas Krejci
 * @since 1.2.0
 */
class TransactionLockingProperty<T> implements Property<T>, WrappedProperty<Property<T>> {
    private final Property<T> property;
    protected final TransactionLockingGraph graph;

    public TransactionLockingProperty(Property<T> property, TransactionLockingGraph graph) {
        this.property = property;
        this.graph = graph;
    }

    @Override public String key() {
        return property.key();
    }

    @Override public T value() throws NoSuchElementException {
        return property.value();
    }

    @Override public boolean isPresent() {
        return property.isPresent();
    }

    @Override public void ifPresent(Consumer<? super T> consumer) {
        property.ifPresent(consumer);
    }

    @Override public T orElse(T otherValue) {
        return property.orElse(otherValue);
    }

    @Override public T orElseGet(Supplier<? extends T> valueSupplier) {
        return property.orElseGet(valueSupplier);
    }

    @Override public <E extends Throwable> T orElseThrow(Supplier<? extends E> exceptionSupplier) throws E {
        return property.orElseThrow(exceptionSupplier);
    }

    @Override public Element element() {
        return property.element();
    }

    @Override public void remove() {
        graph.tx().lockForWriting();
        property.remove();
    }

    @Override public Property<T> getBaseProperty() {
        return property;
    }
}
