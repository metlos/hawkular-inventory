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

import java.util.NoSuchElementException;

import org.apache.tinkerpop.gremlin.process.traversal.Step;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.step.Mutating;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.AbstractStep;

/**
 * Make sure to turn the transaction into write mode prior to any modifications made by the traversal.
 *
 * @author Lukas Krejci
 * @since 1.2.0
 */
final class TransactionLockingStrategy implements TraversalStrategy.DecorationStrategy {
    @Override public void apply(Traversal.Admin<?, ?> traversal) {
        for (int i = 0; i < traversal.getSteps().size(); ++i) {
            Step<?, ?> step = traversal.getSteps().get(i);
            if (step instanceof Mutating) {
                traversal.addStep(i, new LockTransactionStep<>(traversal));
                break;
            }
        }
    }

    private static final class LockTransactionStep<S> extends AbstractStep<S, S> {

        private boolean first = true;

        public LockTransactionStep(Traversal.Admin<?, ?> traversal) {
            super(traversal);
        }

        @Override protected Traverser.Admin<S> processNextStart() throws NoSuchElementException {
            while (true) {
                if (first) {
                    ((TransactionLockingGraph) traversal.getGraph().get()).tx().lockForWriting();
                    first = false;
                }
                return this.starts.next();
            }
        }
    }
}
