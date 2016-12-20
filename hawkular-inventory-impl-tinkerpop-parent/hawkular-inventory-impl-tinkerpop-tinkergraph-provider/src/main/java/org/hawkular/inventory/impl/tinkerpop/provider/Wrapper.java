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
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

/**
 * @author Lukas Krejci
 * @since 1.2.0
 */
final class Wrapper {

    private Wrapper() {

    }

    static <T> Iterator<Property<T>> wrapProperties(TransactionLockingGraph graph, Iterator<Property<T>> props) {
        return orderedStream(props).<Property<T>>map(p -> new TransactionLockingProperty<>(p, graph)).iterator();
    }

    static <T> Iterator<VertexProperty<T>> wrapVertexProperties(TransactionLockingGraph graph,
                                                                       Iterator<VertexProperty<T>> props) {
        return orderedStream(props).<VertexProperty<T>>map(p -> new TransactionLockingVertexProperty<>(p, graph))
                .iterator();
    }

    static Iterator<Edge> wrapEdges(TransactionLockingGraph graph, Iterator<Edge> edges) {
        return orderedStream(edges).<Edge>map(e -> new TransactionLockingEdge(e, graph)).iterator();
    }

    static Iterator<Vertex> wrapVertices(TransactionLockingGraph graph, Iterator<Vertex> vertices) {
        return orderedStream(vertices).<Vertex>map(e -> new TransactionLockingVertex(e, graph)).iterator();
    }

    static <T> Stream<T> orderedStream(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    static Vertex unwrap(Vertex vertex) {
        if (vertex instanceof TransactionLockingVertex) {
            return ((TransactionLockingVertex) vertex).getBaseVertex();
        } else {
            return vertex;
        }
    }
}
