/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.inventory.impl.tinkerpop.provider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration.MapConfiguration;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.hawkular.inventory.api.Configuration;
import org.hawkular.inventory.impl.tinkerpop.spi.GraphProvider;
import org.hawkular.inventory.impl.tinkerpop.spi.IndexSpec;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class TinkerGraphProvider implements GraphProvider {

    @Override public boolean isUniqueIndexSupported() {
        return false;
    }

    @Override public boolean needsDraining() {
        return true;
    }

    @Override public boolean isPreferringBigTransactions() {
        return true;
    }

    @Override public boolean isTransactionRetryWarranted(Graph graph, Throwable t) {
        return t instanceof GraphLockedException;
    }

    @Override
    public TransactionLockingGraph instantiateGraph(Configuration configuration) {
        return new TransactionLockingGraph(new MapConfiguration(
                configuration.getImplementationConfiguration(Arrays.asList(PropertyKey.values()))));
    }

    @Override
    public void ensureIndices(Graph graph, IndexSpec... indexSpecs) {
        //don't bother with this for a demo graph
    }

    @SuppressWarnings("unused")
    enum PropertyKey implements Configuration.Property {
        LOCATION("hawkular.inventory.tinkerpop.data.location", "hawkular.inventory.tinkerpop.data.location", null),
        COMPACTION_INTERVAL("hawkular.inventory.tinkerpop.compactionInterval",
                "hawkular.inventory.tinkerpop.compactionInterval", null)
        ;

        private final String propertyName;
        private final List<String> sysPropName;
        private final List<String> envVarName;

        PropertyKey(String propertyName, String sysPropName, String envVarName) {
            this.envVarName = envVarName == null ? Collections.emptyList() : Collections.singletonList(envVarName);
            this.propertyName = propertyName;
            this.sysPropName = sysPropName == null ? Collections.emptyList() : Collections.singletonList(sysPropName);
        }


        @Override
        public String getPropertyName() {
            return propertyName;
        }

        @Override
        public List<String> getSystemPropertyNames() {
            return sysPropName;
        }

        @Override
        public List<String> getEnvironmentVariableNames() {
            return envVarName;
        }
    }

}
