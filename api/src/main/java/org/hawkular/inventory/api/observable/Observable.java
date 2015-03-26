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

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public interface Observable {
    <C> void subscribe(Observer observer, Action<C> action);

    <C> void unsubscribe(Observer observer, Action<C> action);

    /**
     * While this is an interface so that it is possible to define different actions with different context types,
     * the actions are to be understood as an enumeration.
     *
     * I.e. it is required that all actions of given type are considered equal.
     *
     * <p>You can consider inheriting from the {@link Actions.Base} that
     * satisfies this contract.
     *
     * @param <C> the type of the context object required
     */
    interface Action<C> {
        /**
         * If the other object is an instance of the same class as this action, this method must return true.
         *
         * @param other the object object to compare the equality with
         * @return true if the object is the same class as this action, false otherwise
         */
        boolean equals(Object other);

        /**
         * All instances of the same class should return the same value.
         *
         * @return the degraded hash code
         */
        int hashCode();
    }
}
