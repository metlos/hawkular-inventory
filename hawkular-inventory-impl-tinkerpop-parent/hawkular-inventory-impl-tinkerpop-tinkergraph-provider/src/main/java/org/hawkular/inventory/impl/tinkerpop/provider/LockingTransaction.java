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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.AbstractTransaction;
import org.apache.tinkerpop.gremlin.structure.util.Attachable;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedFactory;

/**
 * @author Lukas Krejci
 * @since 1.2.0
 */
final class LockingTransaction extends AbstractTransaction {
    private static final AtomicLong CNT = new AtomicLong();

    private final ReentrantReadWriteLock txLock;
    private final List<MutationEvent> mutations = new ArrayList<>();
    private final TransactionLockingGraph graph;
    private final File dataDir;

    LockingTransaction(TransactionLockingGraph graph, ReentrantReadWriteLock txLock, File dataDir) {
        super(graph);
        this.graph = graph;
        this.txLock = txLock;
        this.dataDir = dataDir;
    }

    void registerMutation(Element oldValue, Element newValue) {
        mutations.add(new MutationEvent(copyOf(oldValue), copyOf(newValue)));
    }

    <T> void registerMutation(Property<T> oldValue, Property<T> newValue) {
        mutations.add(new MutationEvent(copyOf(oldValue), copyOf(newValue)));
    }

    void lockForWriting() {
        boolean lockForRead = txLock.getReadLockCount() > 0;
        boolean lockedForWrite = txLock.getWriteHoldCount() > 0;

        if (!lockForRead && !lockedForWrite) {
            throw new IllegalStateException("No transaction active.");
        }

        if (lockForRead) {
            txLock.readLock().unlock();
        }

        if (!lockedForWrite) {
            txLock.writeLock().lock();
        }
    }

    List<File> getNonCompactedCommitFiles() {
        File[] files = dataDir.listFiles(child -> child.getName().startsWith("commit-"));
        if (files == null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(files);
        }
    }

    /**
     * @return true if the graph has been modified and needs to be reopened, false otherwise
     */
    boolean compactCommitLog() {
        int prefixLength = "commit-".length();

        List<File> commitFiles = getNonCompactedCommitFiles();
        if (commitFiles.isEmpty()) {
            return false;
        }

        Log.LOG.compactingCommitLog(commitFiles.size());

        commitFiles.sort((a, b) -> {

            String aSuffix = a.getName().substring(prefixLength);
            String bSuffix = b.getName().substring(prefixLength);

            Long aOrd = Long.parseLong(aSuffix);
            Long bOrd = Long.parseLong(bSuffix);

            long diff = aOrd - bOrd;

            return diff > 0 ? 1 : (diff == 0 ? 0 : -1);
        });

        for (File f : commitFiles) {
            Log.LOG.debugf("Processing commit log %s", f);

            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
                @SuppressWarnings("unchecked")
                List<MutationEvent> commitEvents = (List<MutationEvent>) in.readObject();

                applyMutations(commitEvents);
            } catch (IOException | ClassNotFoundException e) {
                throw new IllegalStateException("Cannot read the commit log.", e);
            }
        }

        graph.getBaseGraph().close();

        commitFiles.forEach(File::delete);

        return true;
    }

    @Override protected void doOpen() {
        try {
            txLock.readLock().tryLock(10, TimeUnit.SECONDS);
            Log.LOG.debugf("Started a transaction.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GraphLockedException();
        }

        if (txLock.getReadHoldCount() > 1) {
            txLock.readLock().unlock();
            throw new IllegalStateException("Nested transaction detected");
        }
    }

    @Override protected void doCommit() throws TransactionException {
        persistMutations();
        mutations.clear();
        Log.LOG.debug("Committed a transaction");
    }

    @Override protected void doRollback() throws TransactionException {
        revertMutations();
        mutations.clear();
        Log.LOG.debug("Rolled back a transaction");
    }

    @Override protected void doClose() {
        if (txLock.getWriteHoldCount() == 1) {
            txLock.writeLock().unlock();
        } else if (txLock.getReadHoldCount() == 1) {
            txLock.readLock().unlock();
        } else {
            throw new IllegalStateException("No transaction active");
        }
    }

    @Override protected void doReadWrite() {
    }

    @Override protected void fireOnCommit() {
    }

    @Override protected void fireOnRollback() {
    }

    @Override public Transaction onReadWrite(Consumer<Transaction> consumer) {
        return this;
    }

    @Override public Transaction onClose(Consumer<Transaction> consumer) {
        return this;
    }

    @Override public void addTransactionListener(Consumer<Status> listener) {
    }

    @Override public void removeTransactionListener(Consumer<Status> listener) {
    }

    @Override public void clearTransactionListeners() {
    }

    @Override public boolean isOpen() {
        return txLock.getReadHoldCount() > 0 || txLock.getWriteHoldCount() > 0;
    }

    private void persistMutations() {
        if (!mutations.isEmpty()) {
            File commitLog = new File(dataDir, "commit-" + CNT.incrementAndGet());
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(commitLog))) {
                out.writeObject(mutations);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot persist the commit.", e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T copyOf(T el) {
        if (el == null) {
            return null;
        }

        if (el instanceof Attachable) {
            if (el instanceof Property && ((Property<?>) el).value() == null) {
                return null;
            }
            return el;
        }

        if (el instanceof Vertex) {
            Vertex v = (Vertex) el;
            return (T) DetachedFactory.detach(v, true);
        } else if (el instanceof Edge) {
            Edge e = (Edge) el;
            return (T) DetachedFactory.detach(e, true);
        } else if (el instanceof Property) {
            Property<?> p = (Property<?>) el;
            return (T) (p.isPresent() && p.value() != null
                    ? DetachedFactory.detach(p)
                    : null);
        }

        return el;
    }

    private void applyMutations(List<MutationEvent> mutations) {
        for (MutationEvent ev : mutations) {
            Log.LOG.debugf("Applying mutation of %s to %s", ev.oldValue, ev.newValue);

            if (ev.isVertex()) {
                if (ev.oldValue == null) {
                    //create
                    Vertex v = (Vertex) ev.newValue;
                    graph.getBaseGraph().addVertex(keyValues(v));
                } else {
                    //delete
                    Vertex inGraph = graph.getBaseGraph().vertices(((Vertex) ev.oldValue).id()).next();
                    inGraph.remove();
                }
            } else if (ev.isEdge()) {
                if (ev.oldValue == null) {
                    //create
                    Edge edge = (Edge) ev.newValue;
                    Vertex oldSource = edge.outVertex();
                    Vertex oldTarget = edge.inVertex();

                    //find the source and target in the current graph
                    Vertex newSource = nextOrNull(graph.vertices(oldSource.id()));
                    Vertex newTarget = nextOrNull(graph.vertices(oldTarget.id()));

                    if (newSource == null || newTarget == null) {
                        throw new IllegalStateException("Could not create an edge during commit compaction due to" +
                                " missing vertices in the current graph: " + edge);
                    }

                    newSource.addEdge(edge.label(), newTarget, keyValues(edge));
                } else {
                    //delete
                    Edge inGraph = graph.getBaseGraph().edges(((Edge) ev.newValue).id()).next();
                    inGraph.remove();
                }
            } else if (ev.isProperty()) {
                Property<?> oldP = (Property<?>) ev.oldValue;
                Property<?> newP = (Property<?>) ev.newValue;

                boolean oldPresent = oldP != null && oldP.value() != null;
                boolean newPresent = newP != null && newP.value() != null;

                Element oldE = null;
                Element newE = null;
                if (oldPresent) {
                    oldE = oldP.element() instanceof Vertex
                            ? graph.getBaseGraph().vertices(oldP.element().id()).next()
                            : graph.getBaseGraph().edges(oldP.element().id()).next();
                }

                if (newPresent) {
                    newE = newP.element() instanceof Vertex
                            ? graph.getBaseGraph().vertices(newP.element().id()).next()
                            : graph.getBaseGraph().edges(newP.element().id()).next();
                }

                if (oldPresent) {
                    if (newPresent) {
                        //update
                        newE.property(oldP.key(), newP.value());
                    } else {
                        //delete
                        oldE.property(oldP.key()).remove();
                    }
                } else if (newPresent) {
                    //create
                    newE.property(newP.key(), newP.value());
                }
            }
        }
    }

    private void revertMutations() {
        Log.LOG.debugf("Restoring data because of rollback. Reverting %d mutations.", mutations.size());

        for (int i = mutations.size() - 1; i >= 0; --i) {
            MutationEvent ev = mutations.get(i);

            Log.LOG.debugf("Reverting mutation of %s to %s", ev.oldValue, ev.newValue);

            if (ev.isVertex()) {
                if (ev.oldValue == null) {
                    //created, so we need to delete it
                    Vertex inGraph = graph.getBaseGraph().vertices(((Vertex) ev.newValue).id()).next();
                    inGraph.remove();
                } else {
                    //deleted, so we need to create it
                    graph.getBaseGraph().addVertex(keyValues((Vertex) ev.oldValue));
                }
            } else if (ev.isEdge()) {
                if (ev.oldValue == null) {
                    //created, so we need to delete it
                    Edge inGraph = graph.getBaseGraph().edges(((Edge) ev.newValue).id()).next();
                    inGraph.remove();
                } else {
                    //deleted, so we need to create it
                    Edge edge = (Edge) ev.oldValue;
                    Vertex oldSource = edge.outVertex();
                    Vertex oldTarget = edge.inVertex();

                    //find the source and target in the current graph
                    Vertex newSource = nextOrNull(graph.vertices(oldSource.id()));
                    Vertex newTarget = nextOrNull(graph.vertices(oldTarget.id()));

                    if (newSource == null || newTarget == null) {
                        throw new IllegalStateException("Could not restore an edge during rollback due to missing" +
                                " vertices in the current graph: " + edge);
                    }

                    newSource.addEdge(edge.label(), newTarget, keyValues(edge));
                }
            } else if (ev.isProperty()) {
                Property<?> oldP = (Property<?>) ev.oldValue;
                Property<?> newP = (Property<?>) ev.newValue;

                boolean oldPresent = oldP != null && oldP.value() != null;
                boolean newPresent = newP != null && newP.value() != null;

                Element oldE = null;
                Element newE = null;
                if (oldPresent) {
                    oldE = oldP.element() instanceof Vertex
                            ? graph.getBaseGraph().vertices(oldP.element().id()).next()
                            : graph.getBaseGraph().edges(oldP.element().id()).next();
                }

                if (newPresent) {
                    newE = newP.element() instanceof Vertex
                            ? graph.getBaseGraph().vertices(newP.element().id()).next()
                            : graph.getBaseGraph().edges(newP.element().id()).next();
                }

                if (oldPresent) {
                    if (newPresent) {
                        //update, restore the old value
                        newE.property(oldP.key(), oldP.value());
                    } else {
                        //delete, create the old prop again
                        oldE.property(oldP.key(), oldP.value());
                    }
                } else if (newPresent) {
                    //create, delete the new prop
                    newE.property(newP.key()).remove();
                }
            }
        }

        Log.LOG.debugf("Revert done.");
    }

    private Object[] keyValues(Element el) {
        return ElementHelper.getProperties(el, true, true, Collections.emptySet());
    }

    private <T> T nextOrNull(Iterator<T> it) {
        return it.hasNext() ? it.next() : null;
    }

    private static final class MutationEvent implements Serializable {
        final Object oldValue;
        final Object newValue;

        MutationEvent(Object oldValue, Object newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        boolean isVertex() {
            return oldValue instanceof Vertex || newValue instanceof Vertex;
        }

        boolean isEdge() {
            return oldValue instanceof Edge || newValue instanceof Edge;
        }

        boolean isProperty() {
            return oldValue instanceof Property || newValue instanceof Property;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MutationEvent)) return false;

            MutationEvent that = (MutationEvent) o;

            //explicitly check nullity before the equals check, because some of the Tinkerpop classes don't support
            //equality checks with null...
            if ((oldValue != null && (that.oldValue == null || !oldValue.equals(that.oldValue)))
                || (oldValue == null && that.oldValue != null)) {
                return false;
            }

            //noinspection RedundantIfStatement
            if ((newValue != null && (that.newValue == null || !newValue.equals(that.newValue)))
                    || (newValue == null && that.newValue != null)) {
                return false;
            }

            return true;
        }

        @Override public int hashCode() {
            int result = oldValue != null ? oldValue.hashCode() : 0;
            result = 31 * result + (newValue != null ? newValue.hashCode() : 0);
            return result;
        }
    }
}
