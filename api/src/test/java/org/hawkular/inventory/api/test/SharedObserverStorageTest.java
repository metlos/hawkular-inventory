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
import org.hawkular.inventory.api.filters.Path;
import org.hawkular.inventory.api.filters.Related;
import org.hawkular.inventory.api.model.Environment;
import org.hawkular.inventory.api.model.Tenant;
import org.hawkular.inventory.api.observable.Action;
import org.hawkular.inventory.api.observable.Contexts;
import org.hawkular.inventory.api.observable.ObservableInventory;
import org.hawkular.inventory.api.observable.Observer;
import org.hawkular.inventory.api.observable.ObserverNotificationStrategy;
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
    private Observer<?> observer;

    @Before
    public void instantiate() {
        storage = new SharedObserverStorage();
        observer = Mockito.mock(Observer.class, "observer");
    }

    @Test
    public void canBeRetrievedWithTheSamePath() throws Exception {
        Path path = Path.builder().add(Filter.byTypeAndId(Tenant.class, "kachna")).build();

        storage.addObserver(observer, Action.CREATE, path);

        Set<Observer<Object>> observers = storage.getObservers(Action.CREATE, path);

        Assert.assertEquals(1, observers.size());
        Assert.assertSame(observer, observers.iterator().next());
    }

    @Test
    public void canBeRemovedWithTheSamePath() throws Exception {
        Path path = Path.builder().add(Filter.byTypeAndId(Tenant.class, "kachna")).build();

        storage.addObserver(observer, Action.CREATE, path);
        storage.removeObserver(observer, Action.CREATE, path);

        Set<Observer<Object>> observers = storage.getObservers(Action.CREATE, path);

        Assert.assertEquals(0, observers.size());
    }

    @Test
    public void canBeRetrievedWithMoreGenericPath() throws Exception {
        Path path = Path.builder().add(Filter.byTypeAndId(Tenant.class, "tenant").and(Related.by("contains"))
                .andType(Environment.class).andId("env")).build();

        storage.addObserver(observer, Action.CREATE, path);

        Path retrievalPath = Path.builder().add(Filter.byTypeAndId(Tenant.class, "tenant").and(Related.by("contains"))
                .andType(Environment.class)).build();

        Set<Observer<Object>> observers = storage.getObservers(Action.CREATE, retrievalPath);

        Assert.assertEquals(1, observers.size());
        Assert.assertEquals(observer, observers.iterator().next());

        retrievalPath = Path.builder().add(Filter.byTypeAndId(Tenant.class, "tenant").and(Related.by("contains")))
                .build();
        observers = storage.getObservers(Action.CREATE, retrievalPath);

        Assert.assertEquals(1, observers.size());
        Assert.assertEquals(observer, observers.iterator().next());

        retrievalPath = Path.builder().add(Filter.byTypeAndId(Tenant.class, "tenant")).build();
        observers = storage.getObservers(Action.CREATE, retrievalPath);

        Assert.assertEquals(0, observers.size());
    }

    @Test
    public void cannotBeRemovedWithMoreGenericPath() throws Exception {
        Path addPath = Path.builder().add(Filter.byTypeAndId(Tenant.class, "tenant").and(Related.by("contains"))
                .andType(Environment.class).andId("env")).build();

        storage.addObserver(observer, Action.CREATE, addPath);

        Path delPath = Path.builder().add(Filter.byTypeAndId(Tenant.class, "tenant").and(Related.by("contains"))
                .andType(Environment.class).andId("env")).build();

        storage.removeObserver(observer, Action.CREATE, delPath);

        Assert.assertEquals(0, storage.getObservers(Action.CREATE, addPath).size());

        storage.addObserver(observer, Action.CREATE, addPath);

        delPath = Path.builder().add(Filter.byTypeAndId(Tenant.class, "tenant").and(Related.by("contains"))).build();

        storage.removeObserver(observer, Action.CREATE, delPath);

        Assert.assertEquals(1, storage.getObservers(Action.CREATE, addPath).size());

        ObservableInventory oi = new ObservableInventory(null, new ObserverNotificationStrategy.Synchronous());

        oi.tenants().onCreate().subscribe(new Observer<Contexts.EntityPath<Tenant>>() {
            @Override
            public void onSuccess(Action action, Contexts.EntityPath<Tenant> actionContext) {
            }

            @Override
            public void onFailure(Throwable error, Action action, Contexts.EntityPath<Tenant> actionContext) {
            }
        });
    }
}
