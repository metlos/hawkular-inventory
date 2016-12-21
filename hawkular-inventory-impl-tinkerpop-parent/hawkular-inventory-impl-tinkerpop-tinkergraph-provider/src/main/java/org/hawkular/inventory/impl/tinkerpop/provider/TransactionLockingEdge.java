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

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedProperty;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedEdge;

/**
 * @author Lukas Krejci
 * @since 1.2.0
 */
final class TransactionLockingEdge implements Edge, WrappedEdge<Edge> {
    private final Edge edge;
    private final TransactionLockingGraph graph;

    public TransactionLockingEdge(Edge edge, TransactionLockingGraph graph) {
        this.edge = edge;
        this.graph = graph;
    }

    @Override public Edge getBaseEdge() {
        return edge;
    }

    public Iterator<Vertex> vertices(Direction direction) {
        return Wrapper.wrapVertices(graph, getBaseEdge().vertices(direction));
    }

    public Vertex outVertex() {
        return new TransactionLockingVertex(getBaseEdge().outVertex(), graph);
    }

    public Vertex inVertex() {
        return new TransactionLockingVertex(getBaseEdge().inVertex(), graph);
    }

    public Iterator<Vertex> bothVertices() {
        return Wrapper.wrapVertices(graph, getBaseEdge().bothVertices());
    }

    public <V> Iterator<Property<V>> properties(String... propertyKeys) {
        return Wrapper.wrapProperties(graph, getBaseEdge().properties(propertyKeys));
    }

    public Object id() {
        return getBaseEdge().id();
    }

    public String label() {
        return getBaseEdge().label();
    }

    public TransactionLockingGraph graph() {
        return graph;
    }

    public Set<String> keys() {
        return getBaseEdge().keys();
    }

    public <V> Property<V> property(String key) {
        return new TransactionLockingProperty<>(getBaseEdge().property(key), graph);
    }

    public <V> Property<V> property(String key, V value) {
        graph.tx().lockForWriting();
        Property<V> old = new DetachedProperty<>(key, getBaseEdge().<V>property(key).orElse(null), getBaseEdge());
        Property<V> newP = getBaseEdge().property(key, value);
        Property<V> ret = new TransactionLockingProperty<>(newP, graph);

        Log.LOG.debugf("Updating property of %s from %s to %s", getBaseEdge(), old, newP);

        graph.tx().registerMutation(old, newP);

        return ret;
    }

    public <V> V value(String key) throws NoSuchElementException {
        return getBaseEdge().value(key);
    }

    public void remove() {
        graph.tx().lockForWriting();

        Log.LOG.debugf("Removing %s", getBaseEdge());

        graph.tx().registerMutation(getBaseEdge(), null);
        getBaseEdge().remove();
    }

    public <V> Iterator<V> values(String... propertyKeys) {
        return getBaseEdge().values(propertyKeys);
    }
}
