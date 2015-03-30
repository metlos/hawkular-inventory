/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.inventory.api.observable;

import org.hawkular.inventory.api.filters.Path;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class SharedObserverStorage {

    private PartialPathStorage storage = new PartialPathStorage();

    public void addObserver(Observer observer, Action action, Path path) {
        PartialPathStorage targetStorage = storage;

        for (Path.Step s : path.getSteps()) {
            PartialPathStorage p = targetStorage.getUnresolvedPaths().get(s);
            if (p == null) {
                p = new PartialPathStorage();
                targetStorage.getUnresolvedPaths().put(s, p);
            }
            targetStorage = p;
        }

        Set<Observer<?>> observers = targetStorage.getObservers().get(action);
        if (observers == null) {
            observers = new HashSet<>();
            targetStorage.getObservers().put(action, observers);
        }
        observers.add(observer);
    }

    public void removeObserver(Observer observer, Action action, Path path) {
        PartialPathStorage targetStorage = storage;

        for (Path.Step s : path.getSteps()) {
            targetStorage = targetStorage.getUnresolvedPaths().get(s);
            if (targetStorage == null) {
                return;
            }
        }

        Set<Observer<?>> observers = targetStorage.getObservers().get(action);
        if (observers == null) {
            return;
        }
        observers.remove(observer);
    }

    @SuppressWarnings("unchecked")
    public <C> Set<Observer<C>> getObservers(Action action, Path path) {
        HashSet<Observer<C>> result = new HashSet<>();
        onMatchingPaths(storage, 0, path.getSteps(), (step, st) -> {
            for (Map.Entry<Path.Step, PartialPathStorage> e : st.getUnresolvedPaths().entrySet()) {
                Boolean superSet = e.getKey().isSupersetOf(step);
                if (superSet != null && superSet) {
                    Set<Observer<?>> observers = e.getValue().getObservers().get(action);
                    if (observers != null) {
                        observers.forEach((o) -> result.add((Observer<C>) o));
                    }
                }
            }
        });
        return result;
    }
    
    private void onMatchingPaths(PartialPathStorage storage, int stepIdx, Path.Step[] steps,
                                 BiConsumer<Path.Step, PartialPathStorage> consumer) {

        if (stepIdx >= steps.length) {
            consumer.accept(null, storage);
        }

        Path.Step s = steps[stepIdx];

        if (stepIdx < steps.length - 1) {
            //we're half-way through the path somewhere.. don't add anything to result yet, just follow all the paths
            //that might be applicable
            for (Map.Entry<Path.Step, PartialPathStorage> e : storage.getUnresolvedPaths().entrySet()) {
                Boolean superSet = e.getKey().isSupersetOf(s);
                if (superSet != null && superSet) {
                    onMatchingPaths(e.getValue(), stepIdx + 1, steps, consumer);
                }
            }
        } else {
            //k, we're processing the last element on the path, so we're processing to the result actually
            consumer.accept(s, storage);
        }
    }

    private static final class PartialPathStorage {
        private Map<Path.Step, PartialPathStorage> unresolvedPaths;
        private EnumMap<Action, Set<Observer<?>>> observers;

        public Map<Action, Set<Observer<?>>> getObservers() {
            if (observers == null) {
                observers = new EnumMap<>(Action.class);
            }
            return observers;
        }

        public Map<Path.Step, PartialPathStorage> getUnresolvedPaths() {
            if (unresolvedPaths == null) {
                unresolvedPaths = new HashMap<>();
            }
            return unresolvedPaths;
        }
    }
}
