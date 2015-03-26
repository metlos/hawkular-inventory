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
package org.hawkular.inventory.api.test;

import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.model.Tenant;
import org.hawkular.inventory.api.observable.Actions;
import org.hawkular.inventory.api.observable.Observer;
import org.hawkular.inventory.api.observable.SharedObserverStorage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public class SharedObserverStorageTest {

    private SharedObserverStorage storage;
    private Observer observer;

    @Before
    public void instantiate() {
        storage = new SharedObserverStorage();
        observer = Mockito.mock(Observer.class, "observer");
    }

    @Test
    public void canBeRetrievedWithTheSamePath() throws Exception {
        Filter[] path = Filter.byTypeAndId(Tenant.class, "kachna").get();

        storage.addObserver(observer, new Actions.CreateAction<>(), path);

        Set<Observer> observers = storage.getObservers(new Actions.CreateAction<>(), path);

        Assert.assertEquals(1, observers.size());
        Assert.assertSame(observer, observers.iterator().next());
    }

    @Test
    public void canBeRemovedWithTheSamePath() throws Exception {
        Filter[] path = Filter.byTypeAndId(Tenant.class, "kachna").get();

        storage.addObserver(observer, new Actions.CreateAction<>(), path);
        storage.removeObserver(observer, new Actions.CreateAction<>(), path);

        Set<Observer> observers = storage.getObservers(new Actions.CreateAction<>(), path);

        Assert.assertEquals(0, observers.size());
    }

    @Test
    public void canBeRetrievedWithMoreGenericPath() throws Exception {

    }

    @Test
    public void cannotBeRemovedWithMoreGenericPath() throws Exception {

    }
}
