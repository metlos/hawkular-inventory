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

import org.hawkular.inventory.api.Relationships;
import org.hawkular.inventory.api.Tenants;
import org.hawkular.inventory.api.model.Environment;
import org.hawkular.inventory.api.model.Feed;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.MetricType;
import org.hawkular.inventory.api.model.MetricUnit;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.model.ResourceType;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
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
        //this is a mock answer to simulate location of an environment under given tenant
        when(InventoryMock.environmentsReadWrite.create(any())).thenReturn(InventoryMock.environmentsSingle)
            .thenReturn(InventoryMock.environmentsSingle).thenThrow(new IllegalStateException());
        when(InventoryMock.environmentsSingle.entity()).thenReturn(new Environment("tenant1", "env1"))
                .thenReturn(new Environment("tenant2", "env2"));

        //set up our observers
        TrackingObserver<Contexts.EntityPath<Environment>> observer2 = new TrackingObserver<>();

        observableInventory.tenants().getAll().environments().onCreate().subscribe(observer.cast());
        observableInventory.tenants().get("tenant1").environments().onCreate().subscribe(observer2.cast());

        observableInventory.tenants().get("tenant1").environments().create("env1");
        observableInventory.tenants().get("tenant2").environments().create("env2");

        Assert.assertEquals(2, observer.successes.get(Action.CREATE).size());
        Assert.assertEquals(1, observer2.successes.get(Action.CREATE).size());

        //now check the failure being tracked
        try {
            observableInventory.tenants().get("tenant1").environments().create("kachna");
            Assert.fail("Attempt to create another environment should have failed.");
        } catch (Exception e) {
            //good
        }

        Assert.assertEquals(1, observer.failures.get(Action.CREATE).size());
        Assert.assertEquals(1, observer2.failures.get(Action.CREATE).size());
    }

    @Test
    public void testObserveCreateMetricType() throws Exception {
        //this is a mock answer to simulate location of an environment under given tenant
        when(InventoryMock.metricTypesReadWrite.create(any())).thenReturn(InventoryMock.metricTypesSingle)
                .thenReturn(InventoryMock.metricTypesSingle).thenThrow(new IllegalStateException());
        when(InventoryMock.metricTypesSingle.entity()).thenReturn(new MetricType("tenant1", "mt1"))
                .thenReturn(new MetricType("tenant2", "mt2"));

        //set up our observers
        TrackingObserver<Contexts.EntityPath<MetricType>> observer2 = new TrackingObserver<>();

        observableInventory.tenants().getAll().metricTypes().onCreate().subscribe(observer.cast());
        observableInventory.tenants().get("tenant1").metricTypes().onCreate().subscribe(observer2.cast());

        observableInventory.tenants().get("tenant1").metricTypes()
                .create(new MetricType.Blueprint("mt1", MetricUnit.BYTE));
        observableInventory.tenants().get("tenant2").metricTypes()
            .create(new MetricType.Blueprint("mt2", MetricUnit.BYTE));

        Assert.assertEquals(2, observer.successes.get(Action.CREATE).size());
        Assert.assertEquals(1, observer2.successes.get(Action.CREATE).size());

        //now check the failure being tracked
        try {
            observableInventory.tenants().get("tenant1").metricTypes()
                    .create(new MetricType.Blueprint("kachna", MetricUnit.BYTE));
            Assert.fail("Attempt to create another environment should have failed.");
        } catch (Exception e) {
            //good
        }

        Assert.assertEquals(1, observer.failures.get(Action.CREATE).size());
        Assert.assertEquals(1, observer2.failures.get(Action.CREATE).size());
    }

    @Test
    public void testObserveFeedsRegistered() throws Exception {
        when(InventoryMock.feedsReadAndRegister.register(any())).thenReturn(InventoryMock.feedsSingle)
                .thenReturn(InventoryMock.feedsSingle).thenReturn(InventoryMock.feedsSingle)
                .thenThrow(new IllegalStateException());
        when(InventoryMock.feedsSingle.entity()).thenReturn(new Feed("tenant1", "env1", "feed1"))
                .thenReturn(new Feed("tenant1", "env2", "feed2"))
                .thenReturn(new Feed("tenant2", "env3", "feed3"));

        TrackingObserver<?> observer2 = new TrackingObserver<>();
        TrackingObserver<?> observer3 = new TrackingObserver<>();
        TrackingObserver<?> observer4 = new TrackingObserver<>();

        observableInventory.tenants().getAll().environments().getAll().feeds().onRegister().subscribe(observer.cast());

        observableInventory.tenants().get("tenant1").environments().getAll().feeds().onRegister()
                .subscribe(observer2.cast());

        observableInventory.tenants().get("tenant2").environments().get("env3").feeds().onRegister()
                .subscribe(observer3.cast());

        observableInventory.tenants().get("tenant2").environments().get("env4").feeds().onRegister()
                .subscribe(observer4.cast());

        observableInventory.tenants().get("tenant1").environments().get("env1").feeds().register("feed1");
        observableInventory.tenants().get("tenant1").environments().get("env2").feeds().register("feed2");
        observableInventory.tenants().get("tenant2").environments().get("env3").feeds().register("feed3");

        Assert.assertEquals(3, observer.successes.get(Action.REGISTER).size());
        Assert.assertEquals(2, observer2.successes.get(Action.REGISTER).size());
        Assert.assertEquals(1, observer3.successes.get(Action.REGISTER).size());
        Assert.assertEquals(0, observer4.successes.size());

        //now check the failure being tracked
        try {
            observableInventory.tenants().get("tenant2").environments().get("env4").feeds().register("asdf");
            Assert.fail("Attempt to create another environment should have failed.");
        } catch (Exception e) {
            //good
        }

        Assert.assertEquals(1, observer.failures.get(Action.REGISTER).size());
        Assert.assertEquals(0, observer2.failures.size());
        Assert.assertEquals(0, observer3.failures.size());
        Assert.assertEquals(1, observer4.failures.get(Action.REGISTER).size());
    }

    @Test
    public void testObserveCreateMetric() throws Exception {
        MetricType mt = new MetricType("tenant", "metricType");
        when(InventoryMock.metricsReadWrite.create(any())).thenReturn(InventoryMock.metricsSingle)
                .thenReturn(InventoryMock.metricsSingle).thenReturn(InventoryMock.metricsSingle)
                .thenThrow(new IllegalStateException());
        when(InventoryMock.metricsSingle.entity()).thenReturn(new Metric("tenant1", "env1", "m1", mt))
                .thenReturn(new Metric("tenant1", "env2", "m2", mt))
                .thenReturn(new Metric("tenant2", "env3", "m3", mt));

        TrackingObserver<?> observer2 = new TrackingObserver<>();
        TrackingObserver<?> observer3 = new TrackingObserver<>();
        TrackingObserver<?> observer4 = new TrackingObserver<>();

        observableInventory.tenants().getAll().environments().getAll().metrics().onCreate().subscribe(observer.cast());
        observableInventory.tenants().get("tenant1").environments().getAll().metrics().onCreate()
                .subscribe(observer2.cast());
        observableInventory.tenants().get("tenant2").environments().get("env3").metrics().onCreate()
                .subscribe(observer3.cast());
        observableInventory.tenants().get("tenant2").environments().get("env4").metrics().onCreate()
                .subscribe(observer4.cast());

        observableInventory.tenants().get("tenant1").environments().get("env1").metrics()
                .create(new Metric.Blueprint(mt, "m1"));
        observableInventory.tenants().get("tenant1").environments().get("env2").metrics()
                .create(new Metric.Blueprint(mt, "m2"));
        observableInventory.tenants().get("tenant2").environments().get("env3").metrics()
                .create(new Metric.Blueprint(mt, "m3"));

        Assert.assertEquals(3, observer.successes.get(Action.CREATE).size());
        Assert.assertEquals(2, observer2.successes.get(Action.CREATE).size());
        Assert.assertEquals(1, observer3.successes.get(Action.CREATE).size());
        Assert.assertEquals(0, observer4.successes.size());

        //now check the failure being tracked
        try {
            observableInventory.tenants().get("tenant2").environments().get("env4").metrics()
                    .create(new Metric.Blueprint(mt, "asdf"));
            Assert.fail("Attempt to create another environment should have failed.");
        } catch (Exception e) {
            //good
        }

        Assert.assertEquals(1, observer.failures.get(Action.CREATE).size());
        Assert.assertEquals(0, observer2.failures.size());
        Assert.assertEquals(0, observer3.failures.size());
        Assert.assertEquals(1, observer4.failures.get(Action.CREATE).size());
    }

    @Test
    public void testObserveCreateResource() throws Exception {
        ResourceType rt = new ResourceType("tenant", "resourceType", "1.0.0");
        when(InventoryMock.resourcesReadWrite.create(any())).thenReturn(InventoryMock.resourcesSingle)
                .thenReturn(InventoryMock.resourcesSingle).thenReturn(InventoryMock.resourcesSingle)
                .thenThrow(new IllegalStateException());
        when(InventoryMock.resourcesSingle.entity()).thenReturn(new Resource("tenant1", "env1", "r1", rt))
                .thenReturn(new Resource("tenant1", "env2", "r2", rt))
                .thenReturn(new Resource("tenant2", "env3", "r3", rt));

        TrackingObserver<?> observer2 = new TrackingObserver<>();
        TrackingObserver<?> observer3 = new TrackingObserver<>();
        TrackingObserver<?> observer4 = new TrackingObserver<>();

        observableInventory.tenants().getAll().environments().getAll().resources().onCreate().subscribe(observer.cast());
        observableInventory.tenants().get("tenant1").environments().getAll().resources().onCreate()
                .subscribe(observer2.cast());
        observableInventory.tenants().get("tenant2").environments().get("env3").resources().onCreate()
                .subscribe(observer3.cast());
        observableInventory.tenants().get("tenant2").environments().get("env4").resources().onCreate()
                .subscribe(observer4.cast());

        observableInventory.tenants().get("tenant1").environments().get("env1").resources()
                .create(new Resource.Blueprint("r1", rt));
        observableInventory.tenants().get("tenant1").environments().get("env2").resources()
                .create(new Resource.Blueprint("r2", rt));
        observableInventory.tenants().get("tenant2").environments().get("env3").resources()
                .create(new Resource.Blueprint("r3", rt));

        Assert.assertEquals(3, observer.successes.get(Action.CREATE).size());
        Assert.assertEquals(2, observer2.successes.get(Action.CREATE).size());
        Assert.assertEquals(1, observer3.successes.get(Action.CREATE).size());
        Assert.assertEquals(0, observer4.successes.size());

        //now check the failure being tracked
        try {
            observableInventory.tenants().get("tenant2").environments().get("env4").resources()
                    .create(new Resource.Blueprint("asdf", rt));
            Assert.fail("Attempt to create another environment should have failed.");
        } catch (Exception e) {
            //good
        }

        Assert.assertEquals(1, observer.failures.get(Action.CREATE).size());
        Assert.assertEquals(0, observer2.failures.size());
        Assert.assertEquals(0, observer3.failures.size());
        Assert.assertEquals(1, observer4.failures.get(Action.CREATE).size());
    }

    @Test
    public void testObserveCreateResourceType() throws Exception {
        when(InventoryMock.resourceTypesReadWrite.create(any())).thenReturn(InventoryMock.resourceTypesSingle)
                .thenReturn(InventoryMock.resourceTypesSingle).thenReturn(InventoryMock.resourceTypesSingle)
                .thenThrow(new IllegalStateException());
        when(InventoryMock.resourceTypesSingle.entity()).thenReturn(new ResourceType("tenant1", "rt1", "1"))
                .thenReturn(new ResourceType("tenant1", "rt2", "1"))
                .thenReturn(new ResourceType("tenant2", "rt3", "1"));

        TrackingObserver<?> observer2 = new TrackingObserver<>();
        TrackingObserver<?> observer3 = new TrackingObserver<>();

        observableInventory.tenants().getAll().resourceTypes().onCreate().subscribe(observer.cast());
        observableInventory.tenants().get("tenant1").resourceTypes().onCreate()
                .subscribe(observer2.cast());
        observableInventory.tenants().get("tenant2").resourceTypes().onCreate()
                .subscribe(observer3.cast());

        observableInventory.tenants().get("tenant1").resourceTypes().create(new ResourceType.Blueprint("rt1", "1"));
        observableInventory.tenants().get("tenant1").resourceTypes().create(new ResourceType.Blueprint("rt2", "1"));
        observableInventory.tenants().get("tenant2").resourceTypes().create(new ResourceType.Blueprint("rt3", "1"));

        Assert.assertEquals(3, observer.successes.get(Action.CREATE).size());
        Assert.assertEquals(2, observer2.successes.get(Action.CREATE).size());
        Assert.assertEquals(1, observer3.successes.get(Action.CREATE).size());

        //now check the failure being tracked
        try {
            observableInventory.tenants().get("tenant2").resourceTypes().create(new ResourceType.Blueprint("asdf", "0"));
            Assert.fail("Attempt to create another environment should have failed.");
        } catch (Exception e) {
            //good
        }

        Assert.assertEquals(1, observer.failures.get(Action.CREATE).size());
        Assert.assertEquals(0, observer2.failures.size());
        Assert.assertEquals(1, observer3.failures.size());
    }

    @Test
    public void testRelationshipCreationObserved() throws Exception {
        Relationships.ReadWrite relationshipsReadWrite = Mockito.mock(Relationships.ReadWrite.class);
        when(InventoryMock.tenantsSingle.relationships(any())).thenReturn(relationshipsReadWrite);
        when(relationshipsReadWrite.linkWith(anyString(), any())).thenReturn(InventoryMock.relationshipsSingle);

        TrackingObserver<?> observer2 = new TrackingObserver<>();
        TrackingObserver<?> observer3 = new TrackingObserver<>();

        observableInventory.tenants().getAll().relationships(Relationships.Direction.both).onCreate()
                .subscribe(observer.cast());
        observableInventory.tenants().getAll().relationships(Relationships.Direction.incoming).onCreate()
                .subscribe(observer2.cast());
        observableInventory.tenants().getAll().relationships(Relationships.Direction.outgoing).onCreate()
                .subscribe(observer3.cast());


        observableInventory.tenants().get("tenant").relationships().linkWith("kachnak", new Tenant("tenant2"));

        Assert.assertEquals(1, observer.successes.get(Action.CREATE).size());
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
