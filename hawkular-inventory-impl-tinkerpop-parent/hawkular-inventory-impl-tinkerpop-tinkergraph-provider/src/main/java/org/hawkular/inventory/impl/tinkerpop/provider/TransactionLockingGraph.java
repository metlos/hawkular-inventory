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
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.io.Io;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedGraph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

/**
 * @author Lukas Krejci
 * @since 1.2.0
 */
final class TransactionLockingGraph implements Graph, WrappedGraph<TinkerGraph> {
    private final ReentrantReadWriteLock txLock = new ReentrantReadWriteLock();
    private final TinkerGraph graph;
    private final LockingTransaction tx = new LockingTransaction(this, txLock);

    static {
        TraversalStrategies defaultStrategies = TraversalStrategies.GlobalCache.getStrategies(Graph.class);

        TraversalStrategies.GlobalCache.registerStrategies(TransactionLockingGraph.class,
                defaultStrategies.addStrategies(new TransactionLockingStrategy(), new ElementWrappingStrategy()));
    }

    TransactionLockingGraph(org.apache.commons.configuration.Configuration configuration) {
        graph = TinkerGraph.open(configuration);
    }

    @Override public TinkerGraph getBaseGraph() {
        return graph;
    }

    public <E extends Element> void createIndex(String key, Class<E> elementClass) {
        graph.createIndex(key, elementClass);
    }

    public <E extends Element> void dropIndex(String key, Class<E> elementClass) {
        graph.dropIndex(key, elementClass);
    }

    public void clear() {
        graph.clear();
    }

    public <E extends Element> Set<String> getIndexedKeys(Class<E> elementClass) {
        return graph.getIndexedKeys(elementClass);
    }

    @Override public Vertex addVertex(Object... keyValues) {
        tx().lockForWriting();
        Vertex v = graph.addVertex(keyValues);

        Log.LOG.debugf("Added %s", v);

        tx().registerMutation(null, v);

        return new TransactionLockingVertex(v, this);
    }

    @Override public void close() {
        while (txLock.getReadHoldCount() > 0) {
            txLock.readLock().unlock();
        }

        while (txLock.getWriteHoldCount() > 0) {
            txLock.writeLock().unlock();
        }
        graph.close();
    }

    @Override public GraphComputer compute() {
        throw new UnsupportedOperationException();
    }

    @Override public <C extends GraphComputer> C compute(Class<C> graphComputerClass) {
        throw new UnsupportedOperationException();
    }

    @Override public org.apache.commons.configuration.Configuration configuration() {
        return graph.configuration();
    }

    @Override public Iterator<Edge> edges(Object... edgeIds) {
        return Wrapper.wrapEdges(this, graph.edges(edgeIds));
    }

    @Override public Features features() {
        return new Features() {
            @Override public GraphFeatures graph() {
                return new GraphFeatures() {
                    @Override
                    public boolean supportsConcurrentAccess() {
                        return false;
                    }

                    @Override
                    public boolean supportsTransactions() {
                        return true;
                    }

                    @Override
                    public boolean supportsThreadedTransactions() {
                        return false;
                    }
                };
            }

            @Override public VertexFeatures vertex() {
                return new VertexFeatures() {
                    @Override public VertexProperty.Cardinality getCardinality(String key) {
                        return getBaseGraph().features().vertex().getCardinality(key);
                    }

                    @Override public boolean supportsAddVertices() {
                        return getBaseGraph().features().vertex().supportsAddVertices();
                    }

                    @Override public boolean supportsRemoveVertices() {
                        return getBaseGraph().features().vertex().supportsRemoveVertices();
                    }

                    @Override public boolean supportsMultiProperties() {
                        return false;
                    }

                    @Override public boolean supportsMetaProperties() {
                        return false;
                    }

                    @Override public VertexPropertyFeatures properties() {
                        return getBaseGraph().features().vertex().properties();
                    }

                    @Override public boolean supportsAddProperty() {
                        return getBaseGraph().features().vertex().supportsAddProperty();
                    }

                    @Override public boolean supportsRemoveProperty() {
                        return getBaseGraph().features().vertex().supportsRemoveProperty();
                    }

                    @Override public boolean supportsUserSuppliedIds() {
                        return getBaseGraph().features().vertex().supportsUserSuppliedIds();
                    }

                    @Override public boolean supportsNumericIds() {
                        return getBaseGraph().features().vertex().supportsNumericIds();
                    }

                    @Override public boolean supportsStringIds() {
                        return getBaseGraph().features().vertex().supportsStringIds();
                    }

                    @Override public boolean supportsUuidIds() {
                        return getBaseGraph().features().vertex().supportsUuidIds();
                    }

                    @Override public boolean supportsCustomIds() {
                        return getBaseGraph().features().vertex().supportsCustomIds();
                    }

                    @Override public boolean supportsAnyIds() {
                        return getBaseGraph().features().vertex().supportsAnyIds();
                    }

                    @Override public boolean willAllowId(Object id) {
                        return getBaseGraph().features().vertex().willAllowId(id);
                    }
                };
            }

            @Override public EdgeFeatures edge() {
                return getBaseGraph().features().edge();
            }
        };
    }

    @Override public String toString() {
        return graph.toString();
    }

    @Override public LockingTransaction tx() {
        return tx;
    }

    @Override public Variables variables() {
        return graph.variables();
    }

    @Override public Iterator<Vertex> vertices(Object... vertexIds) {
        return Wrapper.wrapVertices(this, graph.vertices(vertexIds));
    }

    @Override public Vertex addVertex(String label) {
        return addVertex(T.label, label);
    }

    @Override public <I extends Io> I io(Io.Builder<I> builder) {
        return graph.io(builder);
    }
}
