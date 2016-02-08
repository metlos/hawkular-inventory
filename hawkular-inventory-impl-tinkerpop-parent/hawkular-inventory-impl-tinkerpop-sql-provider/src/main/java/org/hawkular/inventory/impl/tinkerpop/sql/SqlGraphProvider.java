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
package org.hawkular.inventory.impl.tinkerpop.sql;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.configuration.MapConfiguration;
import org.hawkular.inventory.api.Configuration;
import org.hawkular.inventory.base.spi.InventoryBackend;
import org.hawkular.inventory.base.spi.Transaction;
import org.hawkular.inventory.impl.tinkerpop.spi.GraphProvider;
import org.hawkular.inventory.impl.tinkerpop.spi.IndexSpec;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.sql.SqlGraph;

/**
 * @author Lukas Krejci
 * @since 0.13.0
 */
public class SqlGraphProvider implements GraphProvider<SqlGraph> {
    @Override public SqlGraph instantiateGraph(Configuration configuration) {
        try {
            Map<String, String> conf = configuration.prefixedWith("sql.")
                    .getImplementationConfiguration(sysPropsAsProperties());

            String jndi = conf.get("sql.datasource.jndi");
            if (jndi == null || jndi.isEmpty()) {
                return new SqlGraph(new MapConfiguration(conf));
            } else {
                InitialContext ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup(jndi);
                return new SqlGraph(ds, new MapConfiguration(conf));
            }
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

    @Override public void startTransaction(SqlGraph graph, InventoryBackend.Transaction transaction) {
        transaction.getAttachments().put("sql.tx", graph.newTransaction());
    }

    @Override public void commit(SqlGraph graph, Transaction<Element> t) {
        getAttachedTransation(t).commit();
    }

    @Override public void rollback(SqlGraph graph, Transaction<Element> t) {
        getAttachedTransation(t).rollback();
    }

    private TransactionalGraph getAttachedTransation(Transaction<Element> tx) {
        TransactionalGraph tg = (TransactionalGraph) tx.getAttachments().get("sql.tx");
        if (tg == null) {
            throw new IllegalStateException("Could not find an postgresql graph attached to a transaction.");
        }

        return tg;
    }

    private static Set<Configuration.Property> sysPropsAsProperties() {
        return System.getProperties().entrySet().stream().map(e -> new Configuration.Property() {
            @Override public String getPropertyName() {
                return (String) e.getKey();
            }

            @Override public List<String> getSystemPropertyNames() {
                return Collections.singletonList((String) e.getKey());
            }
        }).collect(Collectors.toSet());
    }
}
