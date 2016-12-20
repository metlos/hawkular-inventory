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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

/**
 * @author Lukas Krejci
 * @since 1.2.0
 */
final class TransactionLockingVertexProperty<T> extends TransactionLockingProperty<T> implements VertexProperty<T> {
    public TransactionLockingVertexProperty(VertexProperty<T> property, TransactionLockingGraph graph) {
        super(property, graph);
    }

    @Override public Vertex element() {
        return new TransactionLockingVertex(getBaseProperty().element(), graph);
    }

    @Override public TransactionLockingGraph graph() {
        return (TransactionLockingGraph) getBaseProperty().graph();
    }

    @Override public String label() {
        return getBaseProperty().label();
    }

    @Override public <U> Iterator<Property<U>> properties(String... propertyKeys) {
        return Wrapper.wrapProperties(graph, getBaseProperty().properties(propertyKeys));
    }

    @Override public Object id() {
        return getBaseProperty().id();
    }

    @Override public Set<String> keys() {
        return getBaseProperty().keys();
    }

    @Override public <V> Property<V> property(String key) {
        return new TransactionLockingProperty<>(getBaseProperty().property(key), graph);
    }

    @Override public <V> Property<V> property(String key, V value) {
        graph.tx().lockForWriting();
        return new TransactionLockingProperty<>(getBaseProperty().property(key, value), graph);
    }

    @Override public <V> V value(String key) throws NoSuchElementException {
        return getBaseProperty().value(key);
    }

    @Override public <V> Iterator<V> values(String... propertyKeys) {
        return getBaseProperty().values(propertyKeys);
    }

    @Override public VertexProperty<T> getBaseProperty() {
        return (VertexProperty<T>) super.getBaseProperty();
    }
}
