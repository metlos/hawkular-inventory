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

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.LambdaMapStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.MatchStep;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

/**
 * @author Lukas Krejci
 * @since 1.2.0
 */
final class ElementWrappingStrategy implements TraversalStrategy.DecorationStrategy {

    @Override public void apply(Traversal.Admin<?, ?> traversal) {
        int insertionIdx;
        if (traversal.getEndStep() instanceof MatchStep.MatchEndStep) {
            insertionIdx = traversal.getSteps().size() - 1;
        } else {
            insertionIdx = traversal.getSteps().size();
        }

        traversal.addStep(insertionIdx, new LambdaMapStep<>(traversal, tr -> {
            TransactionLockingGraph graph = traversal.getGraph()
                    .map(gr -> (TransactionLockingGraph) gr)
                    .orElseThrow(() ->
                            new IllegalStateException("Cannot wrap elements to support transactions without a graph."));

            return wrap(tr.get(), graph);
        }));
    }

    @SuppressWarnings("unchecked")
    private static <T, U extends T> T wrap(U object, TransactionLockingGraph graph) {
        if (object instanceof TransactionLockingEdge) {
            return object;
        } else if (object instanceof TransactionLockingVertex) {
            return object;
        } else if (object instanceof TransactionLockingVertexProperty) {
            return object;
        } else if (object instanceof TransactionLockingProperty) {
            return object;
        } else if (object instanceof Edge) {
            return (T) new TransactionLockingEdge((Edge) object, graph);
        } else if (object instanceof Vertex) {
            return (T) new TransactionLockingVertex((Vertex) object, graph);
        } else if (object instanceof VertexProperty) {
            return (T) new TransactionLockingVertexProperty<>((VertexProperty<?>) object, graph);
        } else if (object instanceof Property) {
            return (T) new TransactionLockingProperty<>((Property<?>) object, graph);
        } else {
            return object;
        }
    }
}
