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
 */
package org.hawkular.inventory.impl.tinkerpop.postgresql;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.MapConfiguration;
import org.hawkular.inventory.api.Configuration;
import org.hawkular.inventory.base.spi.Transaction;
import org.hawkular.inventory.impl.tinkerpop.spi.GraphProvider;
import org.hawkular.inventory.impl.tinkerpop.spi.IndexSpec;
import org.postgresql.ds.PGSimpleDataSource;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.sql.SqlGraph;

/**
 * @author Lukas Krejci
 * @since 0.13.0
 */
public class PostgresqlGraphProvider implements GraphProvider<SqlGraph> {
    @Override public SqlGraph instantiateGraph(Configuration configuration) {
        try {
            Map<String, String> conf = configuration.prefixedWith("sql.")
                    .getImplementationConfiguration(EnumSet.allOf(PropertyKey.class));

            conf.putIfAbsent("sql.datasource.class", PGSimpleDataSource.class.getName());
            conf.putIfAbsent("sql.datasource.user", "hawkular");
            conf.putIfAbsent("sql.datasource.password", "hawkular");
            conf.putIfAbsent("sql.datasource.host", "localhost");

            return new SqlGraph(new MapConfiguration(conf));
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not instantiate the SQL graph.", e);
        }
    }

    @Override public void ensureIndices(SqlGraph graph, IndexSpec... indexSpecs) {
        try {
            graph.createSchemaIfNeeded();
            graph.commit();
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Could not create the database schema and indices.", e);
        }
    }

    @Override public void startTransaction(SqlGraph graph, Transaction<Element> transaction) {
        transaction.getAttachments().put("postgresql.tx", graph.newTransaction());
    }

    @Override public void commit(SqlGraph graph, Transaction<Element> t) {
        getAttachedTransation(t).commit();
    }

    @Override public void rollback(SqlGraph graph, Transaction<Element> t) {
        getAttachedTransation(t).rollback();
    }

    private TransactionalGraph getAttachedTransation(Transaction<Element> tx) {
        TransactionalGraph tg = (TransactionalGraph) tx.getAttachments().get("postgresql.tx");
        if (tg == null) {
            throw new IllegalStateException("Could not find an postgresql graph attached to a transaction.");
        }

        return tg;
    }

    private enum PropertyKey implements Configuration.Property {
        user("sql.datasource.user", "sql.datasource.user", null),

        password("sql.datasource.password", "sql.datasource.password", null),

        ssl("sql.datasource.ssl", "sql.datasource.ssl", null),

        sslfactory("sql.datasource.sslfactory", "sql.datasource.sslfactory", null),

        sslfactoryarg("sql.datasource.sslfactoryarg", "sql.datasource.sslfactoryarg", null),

        compatible("sql.datasource.compatible", "sql.datasource.compatible", null),

        recvBufferSize("sql.datasource.recvBufferSize", "sql.datasource.recvBufferSize", null),

        protocolVersion("sql.datasource.protocolVersion", "sql.datasource.protocolVersion", null),

        loglevel("sql.datasource.loglevel", "sql.datasource.loglevel", null),

        charSet("sql.datasource.charSet", "sql.datasource.charSet", null),

        allowEncodingChanges("sql.datasource.allowEncodingChanges", "sql.datasource.allowEncodingChanges", null),

        logUnclosedConnections("sql.datasource.logUnclosedConnections", "sql.datasource.logUnclosedConnections", null),

        binaryTransferEnable("sql.datasource.binaryTransferEnable", "sql.datasource.binaryTransferEnable", null),

        binaryTransferDisable("sql.datasource.binaryTransferDisable", "sql.datasource.binaryTransferDisable", null),

        prepareThreshold("sql.datasource.prepareThreshold", "sql.datasource.prepareThreshold", null),

        preparedStatementCacheQueries("sql.datasource.preparedStatementCacheQueries",
                "sql.datasource.preparedStatementCacheQueries", null),

        preparedStatementCacheSizeMiB("sql.datasource.preparedStatementCacheSizeMiB",
                "sql.datasource.preparedStatementCacheSizeMiB", null),

        defaultRowFetchSize("sql.datasource.defaultRowFetchSize", "sql.datasource.defaultRowFetchSize", null),

        loginTimeout("sql.datasource.loginTimeout", "sql.datasource.loginTimeout", null),

        connectTimeout("sql.datasource.connectTimeout", "sql.datasource.connectTimeout", null),

        socketTimeout("sql.datasource.socketTimeout", "sql.datasource.socketTimeout", null),

        tcpKeepAlive("sql.datasource.tcpKeepAlive", "sql.datasource.tcpKeepAlive", null),

        unknownLength("sql.datasource.unknownLength", "sql.datasource.unknownLength", null),

        stringtype("sql.datasource.stringtype", "sql.datasource.stringtype", null),

        kerberosServerName("sql.datasource.kerberosServerName", "sql.datasource.kerberosServerName", null),

        jaasApplicationName("sql.datasource.jaasApplicationName", "sql.datasource.jaasApplicationName", null),

        ApplicationName("sql.datasource.ApplicationName", "sql.datasource.ApplicationName", null),

        gsslib("sql.datasource.gsslib", "sql.datasource.gsslib", null),

        sspiServiceClass("sql.datasource.sspiServiceClass", "sql.datasource.sspiServiceClass", null),

        useSpnego("sql.datasource.useSpnego", "sql.datasource.useSpnego", null),

        sendBufferSize("sql.datasource.sendBufferSize", "sql.datasource.sendBufferSize", null),

        receiveBufferSize("sql.datasource.receiveBufferSize", "sql.datasource.receiveBufferSize", null),

        readOnly("sql.datasource.readOnly", "sql.datasource.readOnly", null),

        disableColumnSanitiser("sql.datasource.disableColumnSanitiser", "sql.datasource.disableColumnSanitiser", null),

        assumeMinServerVersion("sql.datasource.assumeMinServerVersion", "sql.datasource.assumeMinServerVersion", null),

        currentSchema("sql.datasource.currentSchema", "sql.datasource.currentSchema", null),

        targetServerType("sql.datasource.targetServerType", "sql.datasource.targetServerType", null),

        hostRecheckSeconds("sql.datasource.hostRecheckSeconds", "sql.datasource.hostRecheckSeconds", null),

        loadBalanceHosts("sql.datasource.loadBalanceHosts", "sql.datasource.loadBalanceHosts", null);

        private final String propertyName;
        private final List<String> sysPropNames;
        private final List<String> envVarNames;

        PropertyKey(String propName, String sysProp, String env) {
            this.propertyName = propName;
            this.sysPropNames = sysProp == null ? null : Collections.singletonList(sysProp);
            this.envVarNames = env == null ? null : Collections.singletonList(env);
        }

        @Override public String getPropertyName() {
            return propertyName;
        }

        @Override public List<String> getSystemPropertyNames() {
            return sysPropNames;
        }

        @Override public List<String> getEnvironmentVariableNames() {
            return envVarNames;
        }
    }
}
