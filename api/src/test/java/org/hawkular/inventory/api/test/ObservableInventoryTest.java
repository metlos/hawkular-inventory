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

import org.hawkular.inventory.api.Environments;
import org.hawkular.inventory.api.Inventory;
import org.hawkular.inventory.api.Tenants;
import org.hawkular.inventory.api.model.Environment;
import org.hawkular.inventory.api.model.Tenant;
import org.hawkular.inventory.api.observable.Action;
import org.hawkular.inventory.api.observable.Contexts;
import org.hawkular.inventory.api.observable.ObservableInventory;
import org.hawkular.inventory.api.observable.Observer;
import org.hawkular.inventory.api.observable.ObserverNotificationStrategy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public class ObservableInventoryTest {

    private ObservableInventory observableInventory;
    private TrackingObserver<?> observer;

    @Before
    public void init() {
        InventoryMock.rewire();
        observableInventory = new ObservableInventory(InventoryMock.inventory,
                new ObserverNotificationStrategy.Synchronous());

        observer = new TrackingObserver();
    }

    @Test
    public void testObserveCreateTenant() throws Exception {

        //instruct the mock to behave as if create succeeded the first time, fail with error the second time
        when(InventoryMock.tenantsReadWrite.create(any())).then(invocation -> {
            String id = invocation.getArgumentAt(0, String.class);

            Tenants.Single singleMock = Mockito.mock(Tenants.Single.class);
            when(singleMock.entity()).thenReturn(new Tenant(id));

            return singleMock;
        }).thenThrow(new IllegalStateException());

        observableInventory.tenants().onCreate().subscribe(observer.cast());

        observableInventory.tenants().create("kachna");

        Assert.assertEquals(1, observer.successes.get(Action.CREATE).size());

        try {
            observableInventory.tenants().create("kachna2");
            Assert.fail("This time, tenant creation should have failed.");
        } catch (IllegalStateException e) {
            //expected
        }

        Assert.assertEquals(1, observer.failures.get(Action.CREATE).size());
    }

    @Test
    public void testObserveCreateEnvironment() throws Exception {

//        //setup tenants to operate on 2 tenants
//        when(InventoryMock.tenantsReadWrite.get(any())).thenReturn(InventoryMock.tenantsSingle);
//        when(InventoryMock.tenantsReadWrite.getAll(anyVararg())).thenReturn(InventoryMock.tenantsMultiple);
//        when(InventoryMock.tenantsSingle.entity()).thenReturn(new Tenant("tenant1")).thenReturn(new Tenant("tenant2"));
//        when(InventoryMock.tenantsMultiple.entities()).thenReturn(new HashSet<>(
//                Arrays.asList(new Tenant("tenant1"), new Tenant("tenant2"))));

        //this is a mock answer to simulate location of an environment under given tenant
        Function<String, Answer<Environments.Single>> creationSuccess = (tenantId) -> (invocation) -> {
            String id = invocation.getArgumentAt(0, String.class);

            Environments.Single singleMock = Mockito.mock(Environments.Single.class);
            when(singleMock.entity()).thenReturn(new Environment(tenantId, id));

            return singleMock;
        };

        //we'll try to create 3 environments. First 2 calls will succeed, the third call will fail.
        when(InventoryMock.environmentsReadWrite.create(any())).then(creationSuccess.apply("tenant1"))
                .then(creationSuccess.apply("tenant2")).thenThrow(new IllegalStateException());

        //set up our observers
        TrackingObserver<Contexts.EntityPath<Environment>> observer2 = new TrackingObserver<>();

        observableInventory.tenants().getAll().environments().onCreate().subscribe(observer.cast());
        observableInventory.tenants().get("tenant1").environments().onCreate().subscribe(observer2.cast());

        observableInventory.tenants().get("tenant1").environments().create("env1");
        observableInventory.tenants().get("tenant2").environments().create("env2");

        Assert.assertEquals(2, observer.successes.get(Action.CREATE).size());
        Assert.assertEquals(1, observer2.successes.get(Action.CREATE).size());
    }

    private static class TrackingObserver<C> implements Observer<C> {
        Map<Action, List<Success>> successes = new HashMap<>();
        Map<Action, List<Failure>> failures = new HashMap<>();

        @Override
        public void onSuccess(Action action, C actionContext) {
            List<Success> ss = successes.get(action);
            if (ss == null) {
                ss = new ArrayList<>();
                successes.put(action, ss);
            }
            ss.add(new Success(actionContext));

        }

        @SuppressWarnings("unchecked")
        public <T> T cast() {
            return (T) this;
        }

        @Override
        public void onFailure(Throwable error, Action action, C actionContext) {
            List<Failure> ss = failures.get(action);
            if (ss == null) {
                ss = new ArrayList<>();
                failures.put(action, ss);
            }
            ss.add(new Failure(actionContext, error));
        }

        public static class Success {
            final Object context;

            public Success(Object context) {
                this.context = context;
            }
        }

        public static class Failure {
            final Throwable error;
            final Object context;

            public Failure(Object context, Throwable error) {
                this.context = context;
                this.error = error;
            }
        }
    }
}
