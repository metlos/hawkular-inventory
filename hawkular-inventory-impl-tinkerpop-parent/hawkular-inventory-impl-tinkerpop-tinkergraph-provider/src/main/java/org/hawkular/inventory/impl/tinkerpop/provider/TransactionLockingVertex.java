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
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedProperty;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedVertex;

/**
 * @author Lukas Krejci
 * @since 1.2.0
 */
final class TransactionLockingVertex implements Vertex, WrappedVertex<Vertex> {
    private final Vertex vertex;
    private final TransactionLockingGraph graph;

    public TransactionLockingVertex(Vertex vertex, TransactionLockingGraph graph) {
        this.vertex = vertex;
        this.graph = graph;
    }

    public TransactionLockingGraph graph() {
        return (TransactionLockingGraph) getBaseVertex().graph();
    }

    public <V> VertexProperty<V> property(String key) {
        return new TransactionLockingVertexProperty<>(getBaseVertex().property(key), graph);
    }

    public <V> VertexProperty<V> property(VertexProperty.Cardinality cardinality, String key, V value,
            Object... keyValues) {
        //we don't support meta properties
        if (keyValues.length > 0) {
            throw new UnsupportedOperationException("Meta properties not supported.");
        }

        graph.tx().lockForWriting();

        Property<V> old = new DetachedProperty<>(key, getBaseVertex().<V>property(key).orElse(null), getBaseVertex());
        VertexProperty<V> newP = getBaseVertex().property(cardinality, key, value, keyValues);

        VertexProperty<V> ret = new TransactionLockingVertexProperty<>(newP, graph);

        Log.LOG.debugf("Updating property of %s from %s to %s", getBaseVertex(), old, newP);

        graph.tx().registerMutation(old, newP);

        return ret;
    }

    public Set<String> keys() {
        return getBaseVertex().keys();
    }

    public Edge addEdge(String label, Vertex vertex, Object... keyValues) {
        graph.tx().lockForWriting();
        Edge newE = getBaseVertex().addEdge(label, Wrapper.unwrap(vertex), keyValues);
        Edge ret = new TransactionLockingEdge(newE, graph);

        Log.LOG.debugf("Added edge %s from %s to %s", newE, getBaseVertex(), Wrapper.unwrap(vertex));

        graph.tx().registerMutation(null, newE);
        return ret;
    }

    public void remove() {
        graph.tx().lockForWriting();
        getBaseVertex().edges(Direction.BOTH).forEachRemaining(e -> {
            Log.LOG.debugf("Marking %s as removed before deleting %s", e, getBaseVertex());
            graph.tx().registerMutation(e, null);
        });

        Log.LOG.debugf("Removing %s", getBaseVertex());

        graph.tx().registerMutation(getBaseVertex(), null);

        getBaseVertex().remove();
    }

    @Override public String toString() {
        return getBaseVertex().toString();
    }

    public Iterator<Edge> edges(Direction direction, String... edgeLabels) {
        return Wrapper.wrapEdges(graph, getBaseVertex().edges(direction, edgeLabels));
    }

    public Iterator<Vertex> vertices(Direction direction, String... edgeLabels) {
        return Wrapper.wrapVertices(graph, getBaseVertex().vertices(direction, edgeLabels));
    }

    public <V> Iterator<VertexProperty<V>> properties(String... propertyKeys) {
        return Wrapper.wrapVertexProperties(graph, getBaseVertex().properties(propertyKeys));
    }

    @Override public int hashCode() {
        return getBaseVertex().hashCode();
    }

    public Object id() {
        return getBaseVertex().id();
    }

    public String label() {
        return getBaseVertex().label();
    }

    @Override public boolean equals(Object object) {
        return getBaseVertex().equals(object);
    }

    public <V> VertexProperty<V> property(String key, V value) {
        graph.tx().lockForWriting();

        Property<V> old = new DetachedProperty<>(key, getBaseVertex().<V>property(key).orElse(null), getBaseVertex());
        VertexProperty<V> newP = getBaseVertex().property(key, value);
        VertexProperty<V> ret = new TransactionLockingVertexProperty<>(newP, graph);

        Log.LOG.debugf("Updating property of %s from %s to %s", getBaseVertex(), old, newP);

        graph.tx().registerMutation(old, newP);

        return ret;
    }

    public <V> V value(String key) throws NoSuchElementException {
        return getBaseVertex().value(key);
    }

    public <V> Iterator<V> values(String... propertyKeys) {
        return getBaseVertex().values(propertyKeys);
    }

    public <V> VertexProperty<V> property(String key, V value, Object... keyValues) {
        graph.tx().lockForWriting();

        @SuppressWarnings("unchecked")
        Property<V> old = new DetachedProperty<>(key, (V) getBaseVertex().property(key).orElse(null), getBaseVertex());
        VertexProperty<V> newP = getBaseVertex().property(key, value, keyValues);
        VertexProperty<V> ret = new TransactionLockingVertexProperty<>(newP, graph);

        Log.LOG.debugf("Updating property of %s from %s to %s", getBaseVertex(), old, newP);

        graph.tx().registerMutation(old, newP);

        return ret;
    }

    @Override public Vertex getBaseVertex() {
        return vertex;
    }
}
