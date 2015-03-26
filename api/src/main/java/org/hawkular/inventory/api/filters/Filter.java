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
import org.hawkular.inventory.api.model.EntityVisitor;
import org.hawkular.inventory.api.model.Environment;
import org.hawkular.inventory.api.model.Feed;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.MetricType;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.model.ResourceType;
import org.hawkular.inventory.api.model.Tenant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A base class for filters. Defines no filtering logic in and of itself.
 *
 * <p>The implementations of the Hawkular inventory API are supposed to support filtering by {@link Related},
 * {@link With.Ids} and {@link With.Types}. There is also a sub-class of filters for the relation filtering {@link
 * RelationFilter}.
 *
 * <p>In addition to being used to filter a result set, a list of filters can also be used as a specification of a path
 * in an inventory graph. See {@link #isPathTraversing()} method for a more detailed discussion of the filters as paths.
 *
 * To create these filters, feel free to use the static helper methods defined on {@link With}.
 * <p>
 * Note: Additional information for the library consumers.<br/>
 * Don't extend this class with hope that the new filter will work. This class is extendable only for the benefit of
 * the API implementations that can reuse it internally. For the users of the API, only the subclasses of Filter
 * declared directly in the API are available
 * </p>
 *
 * @author Lukas Krejci
 * @since 1.0
 */
public abstract class Filter {
    private static final Filter[] EMPTY = new Filter[0];

    /**
     * This defines a partial ordering over filters.
     *
     * @param f the filter to compare with this one
     * @return true if this filter matches everything (and more) the provided filter does, false if this filter matches
     * less than the provided filter, null if the two are not comparable (have different type, etc.).
     */
    public abstract Boolean isSupersetOf(Filter f);

    /**
     * In addition to filter a result set, filters are also used as path specifiers (a path to a node can be specified
     * as a list of filters that express a graph traversal to the entity in question). When used as a path, it is
     * important to distinguish between filters that merely filter the possible nodes on the position in the path
     * from filters that specify a "hop" to the next position on the path.
     *
     * <p>Generally speaking, only the {@link Related} filter and its subclasses are traversing the path, all other
     * filter types are pure filters even when used in a path.
     *
     * @return true if this type of filter is type traversing, false otherwise
     */
    public boolean isPathTraversing() {
        return false;
    }

    public static Accumulator by(Filter... filters) {
        return new Accumulator(filters);
    }

    public static Accumulator byTypeAndId(Class<? extends Entity> type, String id) {
        return new Accumulator(With.type(type), With.id(id));
    }

    public static Accumulator byType(Class<? extends Entity> type) {
        return new Accumulator(With.type(type));
    }

    public static Accumulator relatedBy(String name) {
        return new Accumulator(Related.by(name));
    }

    public static Accumulator relatedBy(Relationships.WellKnown name) {
        return relatedBy(name.name());
    }

    public static Filter[] all() {
        return EMPTY;
    }

    public static Filter[] pathTo(Entity entity) {
        return entity.accept(new EntityVisitor<Accumulator, Accumulator>() {
            @Override
            public Accumulator visitTenant(Tenant tenant, Accumulator acc) {
                return acc.and(With.type(Tenant.class)).and(With.id(tenant.getId()));
            }

            @Override
            public Accumulator visitEnvironment(Environment environment, Accumulator acc) {
                return acc.and(With.type(Tenant.class)).and(With.id(environment.getTenantId()))
                        .and(With.type(Environment.class)).and(With.id(environment.getId()));
            }

            @Override
            public Accumulator visitFeed(Feed feed, Accumulator acc) {
                return acc.and(With.type(Tenant.class)).and(With.id(feed.getTenantId()))
                        .and(With.type(Environment.class)).and(With.id(feed.getEnvironmentId()))
                        .and(With.type(Feed.class)).and(With.id(feed.getId()));
            }

            @Override
            public Accumulator visitMetric(Metric metric, Accumulator acc) {
                return acc.and(With.type(Tenant.class)).and(With.id(metric.getTenantId()))
                        .and(With.type(Environment.class)).and(With.id(metric.getEnvironmentId()))
                        .and(With.type(Metric.class)).and(With.id(metric.getId()));
            }

            @Override
            public Accumulator visitMetricType(MetricType type, Accumulator acc) {
                return acc.and(With.type(Tenant.class)).and(With.id(type.getTenantId()))
                        .and(With.type(MetricType.class)).and(With.id(type.getId()));
            }

            @Override
            public Accumulator visitResource(Resource resource, Accumulator acc) {
                return acc.and(With.type(Tenant.class)).and(With.id(resource.getTenantId()))
                        .and(With.type(Environment.class)).and(With.id(resource.getEnvironmentId()))
                        .and(With.type(Resource.class)).and(With.id(resource.getId()));
            }

            @Override
            public Accumulator visitResourceType(ResourceType type, Accumulator acc) {
                return acc.and(With.type(Tenant.class)).and(With.id(type.getTenantId()))
                        .and(With.type(ResourceType.class)).and(With.id(type.getId()));
            }
        }, by()).get();
    }

    public static final class Accumulator {
        private final List<Filter> filters = new ArrayList<>();

        private Accumulator(Filter... fs) {
            for (Filter filter : fs) {
                filters.add(filter);
            }
        }

        public Accumulator and(Filter f) {
            filters.add(f);
            return this;
        }

        public Accumulator and(Filter... fs) {
            Collections.addAll(filters, fs);
            return this;
        }

        public Accumulator andType(Class<? extends Entity> type) {
            return and(With.type(type));
        }

        public Accumulator andId(String id) {
            return and(With.id(id));
        }

        public Filter[] get() {
            return filters.toArray(new Filter[filters.size()]);
        }
    }
}
