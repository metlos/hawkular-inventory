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

import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.observable.Observable.Action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class SharedObserverStorage {

    private PartialPathStorage storage = new PartialPathStorage();

    public void addObserver(Observer observer, Action<?> action, Filter... path) {
        PartialPathStorage targetStorage = storage;

        List<Step> steps = toSteps(path);

        for (Step s : steps) {
            PartialPathStorage p = targetStorage.getUnresolvedPaths().get(s);
            if (p == null) {
                p = new PartialPathStorage();
                targetStorage.getUnresolvedPaths().put(s, p);
            }
            targetStorage = p;
        }

        Set<Observer> observers = targetStorage.getObservers().get(action);
        if (observers == null) {
            observers = new HashSet<>();
            targetStorage.getObservers().put(action, observers);
        }
        observers.add(observer);
    }

    public void removeObserver(Observer observer, Action<?> action, Filter... path) {
        PartialPathStorage targetStorage = storage;

        List<Step> steps = toSteps(path);

        for (Step s : steps) {
            targetStorage = targetStorage.getUnresolvedPaths().get(s);
            if (targetStorage == null) {
                return;
            }
        }

        Set<Observer> observers = targetStorage.getObservers().get(action);
        if (observers == null) {
            return;
        }
        observers.remove(observer);
    }
    
    public Set<Observer> getObservers(Action<?> action, Filter... path) {
        List<Step> steps = toSteps(path);
        HashSet<Observer> result = new HashSet<>();
        addObserversToResult(result, storage, action, 0, steps);
        return result;
    }
    
    private void addObserversToResult(Set<Observer> result, PartialPathStorage storage, Action<?> action,
                                      int stepIdx, List<Step> steps) {

        Step s = steps.get(stepIdx);

        if (stepIdx < steps.size() - 1) {
            //we're half-way through the path somewhere.. don't add anything to result yet, just follow all the paths
            //that might be applicable
            for (Map.Entry<Step, PartialPathStorage> e : storage.getUnresolvedPaths().entrySet()) {
                if (e.getKey().isSupersetOf(f)) {
                    addObserversToResult(result, e.getValue(), action, stepIdx + 1, filters);
                }
            }
        } else {
            //k, we're processing the last element on the path, so we're adding to the result actually
            for (Map.Entry<Step, PartialPathStorage> e : storage.getUnresolvedPaths().entrySet()) {
                if (e.getKey().isSupersetOf(f)) {
                    Set<Observer> observers = e.getValue().getObservers().get(action);
                    if (observers != null) {
                        result.addAll(observers);
                    }
                }
            }
        }
    }

    private List<Step> toSteps(Filter... path) {
        //guesstimates
        List<Step> ret = new ArrayList<>(path.length / 2);
        List<Filter> filtersInStep = new ArrayList<>(2);

        boolean hasTraversed = false;

        for (Filter f : path) {
            if (f.isPathTraversing() != hasTraversed) {
                if (!filtersInStep.isEmpty()) {
                    ret.add(new Step(filtersInStep.toArray(new Filter[filtersInStep.size()])));
                    filtersInStep.clear();
                }
                filtersInStep.add(f);
                hasTraversed = true;
            } else {
                filtersInStep.add(f);
                hasTraversed = false;
            }
        }

        return ret;
    }

    private static final class PartialPathStorage {
        private Map<Step, PartialPathStorage> unresolvedPaths;
        private Map<Action<?>, Set<Observer>> observers;

        public Map<Action<?>, Set<Observer>> getObservers() {
            if (observers == null) {
                observers = new HashMap<>();
            }
            return observers;
        }

        public Map<Step, PartialPathStorage> getUnresolvedPaths() {
            if (unresolvedPaths == null) {
                unresolvedPaths = new HashMap<>();
            }
            return unresolvedPaths;
        }
    }

    private static final class Step {
        private final Filter[] filters;

        public Step(Filter[] filters) {
            this.filters = filters;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Step step = (Step) o;

            //the set of filters in a step is always going to be small so I think we can afford to have this
            //inefficient impl..

            for (Filter f1 : filters) {
                boolean found = false;
                for (Filter f2 : step.filters) {
                    if (Objects.equals(f1, f2)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {
            int ret = 0;
            for (Filter f : filters) {
                ret += f.hashCode();
            }
            return ret;
        }
    }
}
