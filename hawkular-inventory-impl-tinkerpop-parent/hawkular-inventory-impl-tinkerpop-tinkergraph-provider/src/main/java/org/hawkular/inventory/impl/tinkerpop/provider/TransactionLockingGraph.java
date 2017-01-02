/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
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

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
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
public final class TransactionLockingGraph implements Graph, WrappedGraph<TinkerGraph> {
    private static final int DEFAULT_FLUSH_INTERVAL = 60 * 60; //1 hour
    private final ReentrantReadWriteLock txLock = new ReentrantReadWriteLock();
    private TinkerGraph graph;
    private final LockingTransaction tx;
    private final Configuration configuration;
    private final ScheduledExecutorService compactionService;

    static {
        TraversalStrategies defaultStrategies = TraversalStrategies.GlobalCache.getStrategies(Graph.class);

        TraversalStrategies.GlobalCache.registerStrategies(TransactionLockingGraph.class,
                defaultStrategies.addStrategies(new TransactionLockingStrategy(), new ElementWrappingStrategy()));
    }

    TransactionLockingGraph(Configuration configuration) {
        String location = configuration.getString(TinkerGraphProvider.PropertyKey.LOCATION.getPropertyName());
        if (location == null) {
            throw new IllegalArgumentException("TransactionLockingGraph needs a location to persist data.");
        }

        File dataDir = new File(location);
        File graphFile = new File(dataDir, "graph");

        if (!dataDir.exists() && !dataDir.mkdirs()) {
            throw new IllegalStateException("Failed to create directory for graph storage.");
        }

        //make sure we have a writeable configuration...
        MapConfiguration copy = new MapConfiguration(new HashMap<>());
        Iterator<String> it = configuration.getKeys();
        while (it.hasNext()) {
            String key = it.next();
            copy.addProperty(key, configuration.getProperty(key));
        }
        configuration = copy;

        configuration.setProperty(TinkerGraph.GREMLIN_TINKERGRAPH_GRAPH_LOCATION, graphFile.getAbsolutePath());
        configuration.setProperty(TinkerGraph.GREMLIN_TINKERGRAPH_GRAPH_FORMAT, "gryo");

        this.configuration = configuration;

        graph = TinkerGraph.open(configuration);
        tx = new LockingTransaction(this, txLock, dataDir);

        if (tx.compactCommitLog()) {
            this.graph = TinkerGraph.open(configuration);
        }

        //don't allow more compactions to run simultaneously by using a single-threaded executor
        compactionService = Executors.newSingleThreadScheduledExecutor();

        int interval = configuration.getInt(TinkerGraphProvider.PropertyKey.COMPACTION_INTERVAL.getPropertyName(),
                DEFAULT_FLUSH_INTERVAL);

        compactionService.scheduleAtFixedRate(() -> flushToDisk(true), interval, interval, TimeUnit.SECONDS);
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
        compactionService.shutdown();

        int interval = configuration.getInt(TinkerGraphProvider.PropertyKey.COMPACTION_INTERVAL.getPropertyName(),
                DEFAULT_FLUSH_INTERVAL);
        try {
            compactionService.awaitTermination(interval, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //set the flag, but finish our job anyway
            Thread.currentThread().interrupt();
        }

        while (txLock.getReadHoldCount() > 0) {
            txLock.readLock().unlock();
        }

        while (txLock.getWriteHoldCount() > 0) {
            txLock.writeLock().unlock();
        }

        flushToDisk(false);
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

    /**
     * This method assumes it is NOT run as part of normal transaction processing but as a "standalone" method
     * in a separate thread.
     */
    private void flushToDisk(boolean reopen) {
        try {
            txLock.writeLock().lock();

            Log.LOG.runningPeriodicFlushToDisk();

            long startTime = System.nanoTime();

            //right now, the in-memory graph contains all the committed state, so we can just persist the graph and
            //delete all the commit log files.

            this.graph.close();
            List<File> toDel = tx.getNonCompactedCommitFiles();

            Log.LOG.debugf("Deleting %d commit log files.", toDel.size());

            toDel.forEach(File::delete);

            if (Log.LOG.isDebugEnabled()) {
                Log.LOG.debugf("Seeing %d commit files after flush. This should always be 0.",
                        tx.getNonCompactedCommitFiles().size());
            }

            if (reopen) {
                this.graph = TinkerGraph.open(configuration);
            }

            Log.LOG.finishedPeriodicFlush((System.nanoTime() - startTime) / 1_000_000);

            txLock.writeLock().unlock();
        } catch (Throwable t) {
            Log.LOG.periodicFlushFailed(t.getMessage(), t);
            throw t;
        }
    }
}
