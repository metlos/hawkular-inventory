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
import org.hawkular.inventory.api.filters.Filter;
import org.hawkular.inventory.api.model.Entity;
import org.hawkular.inventory.api.model.Feed;

/**
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class Actions {

    private Actions() {

    }

    public static final class PathContext<E> {
        private final Class<E> type;
        private final Filter[] path;

        public PathContext(Class<E> type, Filter[] path) {
            this.type = type;
            this.path = path;
        }

        public Filter[] getPath() {
            return path;
        }

        public Class<E> getType() {
            return type;
        }
    }

    public static class ActionOnPath<E> implements Observable.Action<PathContext<E>> {

        private ActionOnPath() {

        }

        @Override
        public Class<? extends PathContext<E>> getContextType() {
            return PathContext.class;
        }
    }

    public static final class CreateAction<E> extends ActionOnPath<E> {
    }

    public static final class UpdateAction<E> extends ActionOnPath<E> {
    }

    public static final class DeleteAction<E> extends ActionOnPath<E> {
    }

    public static final class CopyAction implements Observable.Action<CopyAction.Environments> {

        @Override
        public Class<Environments> getContextType() {
            return Environments.class;
        }

        public static final class Environments {
            private final Filter[] sourceEnvironmentPath;
            private final String targetEnvironmentId;

            public Environments(Filter[] sourceEnvironmentPath, String targetEnvironmentId) {
                this.sourceEnvironmentPath = sourceEnvironmentPath;
                this.targetEnvironmentId = targetEnvironmentId;
            }

            public Filter[] getSourceEnvironmentPath() {
                return sourceEnvironmentPath;
            }

            public String getTargetEnvironmentId() {
                return targetEnvironmentId;
            }
        }
    }

    public static final class RegisterAction extends ActionOnPath<Feed> {
    }

    public static class LinkAction {
        private LinkAction() {

        }

        public Class<? extends RelationshipEnds> getContextType() {
            return RelationshipEnds.class;
        }

        public static final class RelationshipEnds {
            private final Relationships.Direction direction;
            private final String name;
            private final Filter[] pathToCaller;
            private final Entity otherEnd;

            public RelationshipEnds(String name, Relationships.Direction direction, Filter[] pathToCaller,
                                    Entity otherEnd) {
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

            public Filter[] getPathToCaller() {
                return pathToCaller;
            }
        }
    }

    public static final class LinkedAction extends LinkAction
            implements Observable.Action<LinkAction.RelationshipEnds> {
    }

    public static final class UnlinkedAction extends LinkAction
            implements Observable.Action<LinkAction.RelationshipEnds> {

    }
}
