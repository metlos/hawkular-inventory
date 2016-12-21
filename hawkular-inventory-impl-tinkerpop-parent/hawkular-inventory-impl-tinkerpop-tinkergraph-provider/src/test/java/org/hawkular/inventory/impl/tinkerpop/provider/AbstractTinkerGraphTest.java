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
package org.hawkular.inventory.impl.tinkerpop.provider;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.hawkular.inventory.api.Configuration;
import org.hawkular.inventory.api.Inventory;
import org.hawkular.inventory.api.TransactionFrame;
import org.hawkular.inventory.api.model.Environment;
import org.hawkular.inventory.api.model.Relationship;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.model.ResourceType;
import org.hawkular.inventory.api.model.Tenant;
import org.hawkular.inventory.api.test.AbstractBaseInventoryTestsuite;
import org.hawkular.inventory.base.BaseInventory;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukas Krejci
 * @since 0.11.0
 */
abstract class AbstractTinkerGraphTest extends AbstractBaseInventoryTestsuite<Element> {

    protected static void teardown(BaseInventory<?> inventory) throws Exception {
        String pathName = inventory.getConfiguration().getProperty(new DirProperty(), null);

        Path path = Paths.get(pathName);

        if (!path.toFile().exists()) {
            return;
        }

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static final class DirProperty implements Configuration.Property {

        @Override public String getPropertyName() {
            return "blueprints.tg.directory";
        }

        @Override public List<String> getSystemPropertyNames() {
            return Collections.emptyList();
        }
    }

    @Test
    public void testCreateEntityRollback() throws Exception {
        String tenantId = "testCreateEntityRollback";
        TransactionFrame frame = inventory.newTransactionFrame();
        Inventory inv = frame.boundInventory();

        Tenant tenant = inv.tenants().create(Tenant.Blueprint.builder().withId(tenantId).build()).entity();
        Assert.assertNotNull(tenant);
        Assert.assertEquals(tenantId, tenant.getId());

        frame.rollback();

        Assert.assertFalse(inventory.tenants().get(tenantId).exists());
    }

    @Test
    public void testDeleteEntityRollback() throws Exception {
        String tenantId = "testDeleteEntityRollback";
        try {
            inventory.tenants().create(Tenant.Blueprint.builder().withId(tenantId).build());
            inventory.tenants().update(tenantId, Tenant.Update.builder().withProperty("p1", "v1").build());

            TransactionFrame frame = inventory.newTransactionFrame();
            Inventory inv = frame.boundInventory();

            inv.tenants().delete(tenantId);

            Assert.assertFalse(inv.tenants().get(tenantId).exists());

            frame.rollback();

            Assert.assertTrue(inventory.tenants().get(tenantId).exists());
            Tenant t = inventory.tenants().get(tenantId).entity();

            Assert.assertEquals("v1", t.getProperties().get("p1"));
        } finally {
            if (inventory.tenants().get(tenantId).exists()) {
                inventory.tenants().delete(tenantId);
            }
        }
    }

    @Test
    public void testRelationshipCreateRollback() throws Exception {
        String tenantId = "testRelationshipCreateRollback";
        try {
            Tenant t = inventory.tenants().create(Tenant.Blueprint.builder().withId(tenantId).build()).entity();
            ResourceType rt = inventory.tenants().get(tenantId).resourceTypes()
                    .create(ResourceType.Blueprint.builder().withId("rt").build()).entity();

            TransactionFrame frame = inventory.newTransactionFrame();
            Inventory inv = frame.boundInventory();

            Relationship rel = inv.inspect(t).relationships().linkWith("custom", rt.getPath(), Collections.emptyMap())
                    .entity();

            Assert.assertTrue(inv.inspect(rel).exists());

            frame.rollback();

            Assert.assertFalse(inventory.inspect(rel).exists());
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (inventory.tenants().get(tenantId).exists()) {
                inventory.tenants().delete(tenantId);
            }
        }
    }

    @Test
    public void testRelationshipDeleteRollback() throws Exception {
        String tenantId = "testRelationshipDeleteRollback";
        try {
            Tenant t = inventory.tenants().create(Tenant.Blueprint.builder().withId(tenantId).build()).entity();
            ResourceType rt = inventory.tenants().get(tenantId).resourceTypes()
                    .create(ResourceType.Blueprint.builder().withId("rt").build()).entity();

            Relationship rel = inventory.inspect(t).relationships().linkWith("custom", rt.getPath(), Collections.emptyMap())
                    .entity();

            TransactionFrame frame = inventory.newTransactionFrame();
            Inventory inv = frame.boundInventory();

            inv.inspect(rel).delete();

            Assert.assertFalse(inv.inspect(rel).exists());

            frame.rollback();

            Assert.assertTrue(inventory.inspect(rel).exists());
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (inventory.tenants().get(tenantId).exists()) {
                inventory.tenants().delete(tenantId);
            }
        }
    }

    @Test
    public void testRestorationOfGraphOnRollback() throws Exception {
        String tenantId = "testRestorationOfGraphOnRollback";
        try {
            Tenant t = inventory.tenants().create(Tenant.Blueprint.builder().withId(tenantId).build()).entity();
            ResourceType rt = inventory.inspect(t).resourceTypes()
                    .create(ResourceType.Blueprint.builder().withId("rt").build()).entity();

            Environment e = inventory.tenants().get(tenantId).environments()
                    .create(Environment.Blueprint.builder().withId("env").build()).entity();

            Resource r = inventory.inspect(e).resources()
                    .create(Resource.Blueprint.builder().withId("res").withResourceTypePath(rt.getPath().toString())
                            .build()).entity();

            TransactionFrame frame = inventory.newTransactionFrame();
            Inventory inv = frame.boundInventory();

            Assert.assertTrue(inv.inspect(rt).resources().getAll().entities().contains(r));

            inv.inspect(e).delete();

            Assert.assertFalse(inv.inspect(rt).resources().getAll().entities().contains(r));

            frame.rollback();

            Assert.assertTrue(inv.inspect(rt).resources().getAll().entities().contains(r));
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (inventory.tenants().get(tenantId).exists()) {
                inventory.tenants().delete(tenantId);
            }
        }
    }
}
