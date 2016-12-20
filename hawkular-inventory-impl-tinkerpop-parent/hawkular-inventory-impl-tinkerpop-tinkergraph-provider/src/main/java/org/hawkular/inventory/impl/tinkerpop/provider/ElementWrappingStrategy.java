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
 *
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

            Object element = tr.get();

            if (element instanceof Edge) {
                return new TransactionLockingEdge((Edge) element, graph);
            } else if (element instanceof Vertex) {
                return new TransactionLockingVertex((Vertex) element, graph);
            } else if (element instanceof VertexProperty) {
                return new TransactionLockingVertexProperty<>((VertexProperty<?>) element, graph);
            } else if (element instanceof Property) {
                return new TransactionLockingProperty<>((Property<?>) element, graph);
            } else {
                return element;
            }
        }));
    }
}
