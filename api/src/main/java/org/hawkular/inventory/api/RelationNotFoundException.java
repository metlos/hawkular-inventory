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

package org.hawkular.inventory.api;

import org.hawkular.inventory.api.filters.Path;
import org.hawkular.inventory.api.model.Entity;

/**
 * @author Jirka Kremser
 * @since 1.0
 */
public final class RelationNotFoundException extends InventoryException {

    private final String sourceEntityType;
    private final Path path;
    private final String nameOrId;

    public RelationNotFoundException(Class<? extends Entity> sourceEntityType, String nameOrId, Path path,
                                     String message, Throwable cause) {
        super(message, cause);
        this.sourceEntityType = sourceEntityType != null ? sourceEntityType.getSimpleName() : null;
        this.path = path;
        this.nameOrId = nameOrId;
    }

    public RelationNotFoundException(Class<? extends Entity> sourceEntityType, String nameOrId, Path path,
                                     Throwable cause) {
        this(sourceEntityType, nameOrId, path, null, cause);
    }

    public RelationNotFoundException(String nameOrId, Path path, String message) {
        this(null, nameOrId, path, message, null);
    }

    public RelationNotFoundException(Class<? extends Entity> sourceEntityType, Path path) {
        this(sourceEntityType, null, path, null, null);
    }

    public RelationNotFoundException(String nameOrId, Path path) {
        this(null, nameOrId, path, null, null);
    }

    @Override
    public String getMessage() {
        return "Relation"
                + (sourceEntityType != null ? " with source in " + sourceEntityType : "")
                + (nameOrId != null ? " with name or id '" + nameOrId + "'" : "")
                + (path != null ? " searched on path: " + path : "")
                + (super.getMessage() == null ? ": Was not found." : ": " + super.getMessage());
    }
}
