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
package org.hawkular.inventory.api.filters;

import org.hawkular.inventory.api.Relationships;
import org.hawkular.inventory.api.model.Entity;

import java.util.Arrays;

/**
 * @author Jirka Kremser
 * @since 1.0
 */
public final class RelationWith {
    private RelationWith() {

    }

    public static Ids id(String id) {
        return new Ids(id);
    }

    public static Ids ids(String... ids) {
        return new Ids(ids);
    }

    public static Properties property(String property, String value) {
        return new Properties(property, value);
    }

    public static Properties properties(String property, String... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("there must be at least one value of the property");
        }
        return new Properties(property, values);
    }

    public static Properties name(String value) {
        //TODO this ties the API to tinkerpop impl...
        return new Properties("label", value);
    }

    public static Properties names(String... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("there must be at least one value of the relation name");
        }
        //TODO this ties the API to tinkerpop impl...
        return new Properties("label", values);
    }

    @SafeVarargs
    public static SourceOfType sourcesOfTypes(Class<? extends Entity>... types) {
        return new SourceOfType(types);
    }

    public static SourceOfType sourceOfType(Class<? extends Entity> type) {
        return new SourceOfType(type);
    }

    @SafeVarargs
    public static TargetOfType targetsOfTypes(Class<? extends Entity>... types) {
        return new TargetOfType(types);
    }

    public static TargetOfType targetOfType(Class<? extends Entity> type) {
        return new TargetOfType(type);
    }

    public static Direction direction(Relationships.Direction direction) {
        return new Direction(direction);
    }

    public static final class Ids extends RelationFilter {

        private final String[] ids;

        public Ids(String... ids) {
            this.ids = ids;
        }

        public String[] getIds() {
            return ids;
        }

        @Override
        public String toString() {
            return  "RelationshipIds" + Arrays.asList(ids).toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Ids other = (Ids) o;

            return Arrays.equals(ids, other.ids);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(ids);
        }

        @Override
        public Boolean isSupersetOf(Filter f) {
            if (!(f instanceof Ids)) {
                return null;
            }

            Ids other = (Ids) f;

            return Arrays.asList(ids).containsAll(Arrays.asList(other.ids));
        }
    }

    public static final class Properties extends RelationFilter {

        private final String property;
        private final String[] values;

        public Properties(String property, String... values) {
            this.property = property;
            this.values = values;
        }

        public String getProperty() {
            return property;
        }

        public String[] getValues() {
            return values;
        }

        @Override
        public String toString() {
            return  "RelationshipProperty: " + getProperty() + "=" + Arrays.asList(values).toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Properties that = (Properties) o;

            if (!property.equals(that.property)) return false;
            if (!Arrays.equals(values, that.values)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = property.hashCode();
            result = 31 * result + Arrays.hashCode(values);
            return result;
        }

        @Override
        public Boolean isSupersetOf(Filter f) {
            if (!(f instanceof Properties)) {
                return null;
            }

            Properties other = (Properties) f;

            if (!property.equals(other.property)) {
                return false;
            }

            return Arrays.asList(values).containsAll(Arrays.asList(other.values));
        }
    }

    public static class SourceOrTargetOfType extends RelationFilter {
        private final Class<? extends Entity>[] types;

        public String getFilterName() {
            return "SourceOrTargetOfType";
        }

        @SafeVarargs
        public SourceOrTargetOfType(Class<? extends Entity>... types) {
            this.types = types;
        }

        public Class<? extends Entity>[] getTypes() {
            return types;
        }

        @Override
        public String toString() {
            StringBuilder ret = new StringBuilder(getFilterName() + "[");
            if (types.length > 0) {
                ret.append(types[0].getSimpleName());

                for(int i = 1; i < types.length; ++i) {
                    ret.append(", ").append(types[i].getSimpleName());
                }
            }
            ret.append("]");
            return ret.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SourceOrTargetOfType that = (SourceOrTargetOfType) o;

            return Arrays.equals(types, that.types);
        }

        @Override
        public int hashCode() {
            return types != null ? Arrays.hashCode(types) : 0;
        }

        @Override
        public Boolean isSupersetOf(Filter f) {
            if (this == f) {
                return true;
            }

            if (!this.getClass().isAssignableFrom(f.getClass())) {
                return null;
            }

            SourceOrTargetOfType other = (SourceOrTargetOfType) f;

            return Arrays.asList(types).containsAll(Arrays.asList(other.types));
        }
    }

    public static final class SourceOfType extends SourceOrTargetOfType {

        @SafeVarargs
        public SourceOfType(Class<? extends Entity>... types) {
            super(types);
        }

        @Override
        public String getFilterName() {
            return "SourceOfType";
        }
    }

    public static final class TargetOfType extends SourceOrTargetOfType {

        @SafeVarargs
        public TargetOfType(Class<? extends Entity>... types) {
            super(types);
        }

        @Override
        public String getFilterName() {
            return "TargetOfType";
        }
    }

    public static final class Direction extends RelationFilter {
        private final Relationships.Direction direction;

        public static Direction outgoing() {
            return new Direction(Relationships.Direction.outgoing);
        }

        public static Direction incoming() {
            return new Direction(Relationships.Direction.incoming);
        }

        public Direction(Relationships.Direction direction) {
            this.direction = direction;
        }

        public Relationships.Direction getDirection() {
            return direction;
        }

        @Override
        public Boolean isSupersetOf(Filter f) {
            if (!(f instanceof Direction)) {
                return null;
            }

            Direction other = (Direction) f;

            return direction == other.direction;

        }
    }
}
