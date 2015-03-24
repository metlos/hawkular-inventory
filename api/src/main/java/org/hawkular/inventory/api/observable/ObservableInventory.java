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

import org.hawkular.inventory.api.Configuration;
import org.hawkular.inventory.api.Inventory;
import org.hawkular.inventory.api.Tenants;
import org.hawkular.inventory.api.filters.Filter;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class ObservableInventory implements Inventory {

    private final Inventory inventory;
    private final ObserverNotificationStrategy notificationStrategy;

    public ObservableInventory(Inventory inventory, ObserverNotificationStrategy notificationStrategy) {
        this.inventory = inventory;
        this.notificationStrategy = notificationStrategy;
    }

    @Override
    public void initialize(Configuration configuration) {
        inventory.initialize(configuration);
    }

    @Override
    public Tenants.ReadWrite tenants() {
        return new ObservableTenants.ReadWrite(inventory.tenants(), notificationStrategy, Filter.all());
    }

    @Override
    public void close() throws Exception {
        inventory.close();
    }
}
