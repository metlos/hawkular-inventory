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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A path is an adaptation of list filters to serve as location specification in inventory.
 *
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class Path {

    private final Step[] steps;

    public static Path root() {
        return new Path(new Step[0]);
    }
    public static Builder builder() {
        return new Builder();
    }

    public static Builder extend(Path base) {
        Builder b = new Builder();
        for (Step s : base.getSteps()) {
            b.add(s);
        }

        return b;
    }

    public static Path to(Entity entity) {
        return entity.accept(new EntityVisitor<Builder, Builder>() {
            @Override
            public Builder visitTenant(Tenant tenant, Builder acc) {
                return acc.add(With.type(Tenant.class)).add(With.id(tenant.getId()));
            }

            @Override
            public Builder visitEnvironment(Environment environment, Builder acc) {
                return acc.add(With.type(Tenant.class)).add(With.id(environment.getTenantId()))
                        .add(With.type(Environment.class)).add(With.id(environment.getId()));
            }

            @Override
            public Builder visitFeed(Feed feed, Builder acc) {
                return acc.add(With.type(Tenant.class)).add(With.id(feed.getTenantId()))
                        .add(With.type(Environment.class)).add(With.id(feed.getEnvironmentId()))
                        .add(With.type(Feed.class)).add(With.id(feed.getId()));
            }

            @Override
            public Builder visitMetric(Metric metric, Builder acc) {
                return acc.add(With.type(Tenant.class)).add(With.id(metric.getTenantId()))
                        .add(With.type(Environment.class)).add(With.id(metric.getEnvironmentId()))
                        .add(With.type(Metric.class)).add(With.id(metric.getId()));
            }

            @Override
            public Builder visitMetricType(MetricType type, Builder acc) {
                return acc.add(With.type(Tenant.class)).add(With.id(type.getTenantId()))
                        .add(With.type(MetricType.class)).add(With.id(type.getId()));
            }

            @Override
            public Builder visitResource(Resource resource, Builder acc) {
                return acc.add(With.type(Tenant.class)).add(With.id(resource.getTenantId()))
                        .add(With.type(Environment.class)).add(With.id(resource.getEnvironmentId()))
                        .add(With.type(Resource.class)).add(With.id(resource.getId()));
            }

            @Override
            public Builder visitResourceType(ResourceType type, Builder acc) {
                return acc.add(With.type(Tenant.class)).add(With.id(type.getTenantId()))
                        .add(With.type(ResourceType.class)).add(With.id(type.getId()));
            }
        }, builder()).build();
    }


    public Path(Step[] steps) {
        this.steps = steps;
    }

    public Step[] getSteps() {
        return steps;
    }

    public Path extend(Filter.Accumulator fs) {
        return extend(fs.get());
    }

    public Path extend(Filter... fs) {
        return extend(this).add(fs).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Path path = (Path) o;

        return Arrays.equals(steps, path.steps);

    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(steps);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Path[");
        if (steps.length > 0) {
            sb.append("<").append(steps[0].filters).append(">");
        }
        for (int i = 1; i < steps.length; ++i) {
            sb.append(", <").append(steps[i].filters).append(">");
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * A step in a path. Filters in a step restrict the selection amongst siblings in the inventory graph.
     */
    public static final class Step {
        private final Iterable<Filter> filters;

        public Step(Filter[] filters) {
            this.filters = Arrays.asList(filters);
        }

        public Iterable<Filter> getFilters() {
            return filters;
        }

        public Boolean isSupersetOf(Step other) {
            Boolean result = null;
            for (Filter f1 : filters) {
                for (Filter f2 : other.filters) {
                    Boolean superSet = f1.isSupersetOf(f2);

                    if (superSet != null && !superSet) {
                        return false;
                    }

                    if (superSet != null) {
                        result = true;
                    }
                }
            }

            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Step step = (Step) o;

            //the set of filters in a step is always going to be small so I think we can afford to have this
            //inefficient impl..

            for (Filter f1 : filters) {
                boolean found = false;
                for (Filter f2 : step.filters) {
                    if (Objects.equals(f1, f2)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {
            int ret = 0;
            for (Filter f : filters) {
                ret += f.hashCode();
            }
            return ret;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Step[");
            sb.append("filters=").append(filters);
            sb.append(']');
            return sb.toString();
        }


    }

    public static final class Builder {
        private final List<Step> steps = new ArrayList<>();
        private final Set<Filter> currentStep = new HashSet<>();
        private boolean currentStepOnEntity;

        private Builder() {

        }

        public Builder add(Filter... fs) {
            for (Filter f : fs) {
                addSingle(f);
            }

            return this;
        }

        public Builder add(Filter.Accumulator fs) {
            return add(fs.get());
        }

        public Builder add(Step s) {
            if (canMerge(s)) {
                s.filters.forEach(this::addSingle);
            } else {
                drainCurrentStep();
                steps.add(s);
            }

            return this;
        }

        public Path build() {
            drainCurrentStep();
            return new Path(steps.toArray(new Step[steps.size()]));
        }

        private void addSingle(Filter f) {
            if (f.isEntityFilter() != currentStepOnEntity) {
                drainCurrentStep();
            }

            currentStep.add(f);
            currentStepOnEntity = f.isEntityFilter();
        }

        private boolean canMerge(Step s) {
            if (currentStep.isEmpty()) {
                return true;
            }

            if (!s.filters.iterator().hasNext()) {
                return true;
            }

            return currentStep.iterator().next().isEntityFilter() == s.filters.iterator().next().isEntityFilter();
        }
        private void drainCurrentStep() {
            if (currentStep.isEmpty()) {
                return;
            }

            Step s = new Step(currentStep.toArray(new Filter[currentStep.size()]));
            steps.add(s);

            currentStep.clear();
        }
    }
}
