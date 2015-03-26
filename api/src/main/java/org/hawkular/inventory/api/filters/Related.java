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

/**
 * Defines a filter on entities having specified relationship.
 *
 * @param <T> The type of the entity using which the filter is constructed.
 *
 * @author Lukas Krejci
 * @since 1.0
 */
public class Related<T extends Entity> extends Filter {

    private final T entity;
    private final String relationshipName;
    private final String relationshipId;
    private final EntityRole entityRole;

    /**
     * Specifies a filter for entities that are sources of a relationship with the specified entity.
     *
     * @param entity the entity that is the target of the relationship
     * @param relationship the name of the relationship
     * @param <U> the type of the entity
     * @return a new "related" filter instance
     */
    public static <U extends Entity> Related<U> with(U entity, String relationship) {
        return new Related<>(entity, relationship, EntityRole.SOURCE);
    }

    /**
     * An overloaded version of {@link #with(org.hawkular.inventory.api.model.Entity, String)} that uses one of the
     * {@link org.hawkular.inventory.api.Relationships.WellKnown} as the name of the relationship.
     *
     * @param entity the entity that is the target of the relationship
     * @param relationship the type of the relationship
     * @param <U> the type of the entity
     * @return a new "related" filter instance
     */
    public static <U extends Entity> Related<U> with(U entity, Relationships.WellKnown relationship) {
        return new Related<>(entity, relationship.name(), EntityRole.SOURCE);
    }

    /**
     * Creates a filter for entities that are sources of at least one relationship with the specified name. The target
     * entity is not specified and can be anything.
     *
     * @param relationshipName the name of the relationship
     * @return a new "related" filter instance
     */
    public static Related<?> by(String relationshipName) {
        return new Related<>(null, relationshipName, EntityRole.SOURCE);
    }

    /**
     * Overloaded version of {@link #by(String)} that uses the
     * {@link org.hawkular.inventory.api.Relationships.WellKnown} as the name of the relationship.
     *
     * @param relationship the type of the relationship
     * @return a new "related" filter instance
     */
    public static Related<?> by(Relationships.WellKnown relationship) {
        return new Related<>(null, relationship.name(), EntityRole.SOURCE);
    }

    /**
     * Creates a filter for entities that are sources of at least one relationship with the specified id. The target
     * entity is not specified and can be anything.
     *
     * @param relationshipId the id of the relationship
     * @return a new "related" filter instance
     */
    public static Related<?> byRelationshipWithId(String relationshipId) {
        return new Related<>(null, null, relationshipId, EntityRole.SOURCE);
    }

    /**
     * Specifies a filter for entities that are targets of a relationship with the specified entity.
     *
     * @param entity the entity that is the source of the relationship
     * @param relationship the name of the relationship
     * @param <U> the type of the entity
     * @return a new "related" filter instance
     */
    public static <U extends Entity> Related<U> asTargetWith(U entity, String relationship) {
        return new Related<>(entity, relationship, EntityRole.TARGET);
    }

    /**
     * An overloaded version of {@link #asTargetWith(org.hawkular.inventory.api.model.Entity, String)} that uses one of
     * the {@link org.hawkular.inventory.api.Relationships.WellKnown} as the name of the relationship.
     *
     * @param entity the entity that is the source of the relationship
     * @param relationship the type of the relationship
     * @param <U> the type of the entity
     * @return a new "related" filter instance
     */
    public static <U extends Entity> Related<U> asTargetWith(U entity, Relationships.WellKnown relationship) {
        return new Related<>(entity, relationship.name(), EntityRole.TARGET);
    }

    /**
     * Creates a filter for entities that are targets of at least one relationship with the specified name. The source
     * entity is not specified and can be anything.
     *
     * @param relationshipName the name of the relationship
     * @return a new "related" filter instance
     */
    public static Related<?> asTargetBy(String relationshipName) {
        return new Related<>(null, relationshipName, EntityRole.TARGET);
    }

    /**
     * Overloaded version of {@link #asTargetBy(String)} that uses the
     * {@link org.hawkular.inventory.api.Relationships.WellKnown} as the name of the relationship.
     *
     * @param relationship the type of the relationship
     * @return a new "related" filter instance
     */
    public static Related<?> asTargetBy(Relationships.WellKnown relationship) {
        return new Related<>(null, relationship.name(), EntityRole.TARGET);
    }

    protected Related(T entity, String relationshipName, String relationshipId, EntityRole entityRole) {
        if (relationshipName == null && relationshipId == null) {
            throw new IllegalArgumentException("Either relationshipName or relationshipId must be defined.");
        }
        this.entity = entity;
        this.relationshipName = relationshipName;
        this.relationshipId = relationshipId;
        this.entityRole = entityRole;
    }

    protected Related(T entity, String relationshipName, EntityRole entityRole) {
        this(entity, relationshipName, null, entityRole);
    }

    @Override
    public Boolean isSupersetOf(Filter f) {
        if (!(f instanceof Related)) {
            return null;
        }

        Related<?> other = (Related<?>) f;

        //if this filter has the relationship ID, that it only can match itself
        if (relationshipId != null && !relationshipId.equals(other.relationshipId)) {
            return false;
        }

        //having a null relationship name and null id is not a legal combination...

        //we're relating by name and other is relating by id... we cannot determine if we match...
        if (other.relationshipName == null) {
            return null;
        }

        if (!relationshipName.equals(other.relationshipName)) {
            return false;
        }

        //direction ANY matches both source and target
        switch (entityRole) {
            case SOURCE:
                if (other.entityRole == EntityRole.TARGET) {
                    return false;
                }
                break;
            case TARGET:
                if (other.entityRole == EntityRole.SOURCE) {
                    return false;
                }
                break;
            default:
                break;
        }

        //if entity == null, this filter matches any entity, so it's irrelevant what the other filter does
        //otherwise the entities must match...
        return entity == null || entity.equals(other.entity);
    }

    @Override
    public boolean isPathTraversing() {
        return true;
    }

    /**
     * @return the entity used for creating this filter.
     */
    public T getEntity() {
        return entity;
    }

    /**
     * @return the name of the relationship
     */
    public String getRelationshipName() {
        return relationshipName;
    }

    /**
     * @return the id of the relationship
     */
    public String getRelationshipId() {
        return relationshipId;
    }

    /**
     * @return the role of the entity in the filter
     */
    public EntityRole getEntityRole() {
        return entityRole;
    }

    public static enum EntityRole {
        TARGET, SOURCE, ANY
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + (entity != null ? "entity=" + String.valueOf(entity) : "")
                + ", rel='" + relationshipName + "', role=" + entityRole.name() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Related related = (Related) o;

        if (entity != null ? !entity.equals(related.entity) : related.entity != null) return false;
        if (entityRole != related.entityRole) return false;
        if (relationshipId != null ? !relationshipId.equals(related.relationshipId) : related.relationshipId != null)
            return false;
        if (relationshipName != null ? !relationshipName.equals(related.relationshipName) : related.relationshipName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = entity != null ? entity.hashCode() : 0;
        result = 31 * result + (relationshipName != null ? relationshipName.hashCode() : 0);
        result = 31 * result + (relationshipId != null ? relationshipId.hashCode() : 0);
        result = 31 * result + (entityRole != null ? entityRole.hashCode() : 0);
        return result;
    }
}
