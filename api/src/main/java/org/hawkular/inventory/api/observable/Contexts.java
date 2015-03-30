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

import org.hawkular.inventory.api.Relationships;
import org.hawkular.inventory.api.ResolvableToSingle;
import org.hawkular.inventory.api.filters.Path;
import org.hawkular.inventory.api.model.Entity;
import org.hawkular.inventory.api.model.Relationship;

import java.util.Optional;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class Contexts {

    private Contexts() {

    }

    public static final class EntityPath<E> {
        private final Class<E> type;
        private final Path path;
        private final E entity;

        public EntityPath(Class<E> type, Path path, Optional<? extends ResolvableToSingle<E>> singleInterface) {
            this(type, path, singleInterface.map(ResolvableToSingle::entity).orElse(null));
        }

        public EntityPath(Class<E> type, Path path, E entity) {
            this.type = type;
            this.path = path;
            this.entity = entity;
        }

        public Path getPath() {
            return path;
        }

        public Class<E> getType() {
            return type;
        }

        public E getEntity() {
            return entity;
        }
    }

    public static final class EnvironmentCopy {
        private final Path sourceEnvironmentPath;
        private final String targetEnvironmentId;

        public EnvironmentCopy(Path sourceEnvironmentPath, String targetEnvironmentId) {
            this.sourceEnvironmentPath = sourceEnvironmentPath;
            this.targetEnvironmentId = targetEnvironmentId;
        }

        public Path getSourceEnvironmentPath() {
            return sourceEnvironmentPath;
        }

        public String getTargetEnvironmentId() {
            return targetEnvironmentId;
        }
    }

    public static final class RelationshipLink {
        private final Relationships.Direction direction;
        private final String name;
        private final Path pathToCaller;
        private final Entity otherEnd;

        public RelationshipLink(String name, Relationships.Direction direction, Path pathToCaller, Entity otherEnd) {
            this.name = name;
            this.direction = direction;
            this.pathToCaller = pathToCaller;
            this.otherEnd = otherEnd;
        }

        public String getName() {
            return name;
        }

        public Relationships.Direction getDirection() {
            return direction;
        }

        public Entity getOtherEnd() {
            return otherEnd;
        }

        public Path getPathToCaller() {
            return pathToCaller;
        }
    }
}
