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
package org.hawkular.inventory.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Lukas Krejci
 * @since 1.0
 */
final class ResponseUtil {

    /**
     * This method exists solely to concentrate usage of {@link javax.ws.rs.core.Response#created(java.net.URI)} into
     * one place until <a href="https://issues.jboss.org/browse/RESTEASY-1162">this JIRA</a> is resolved somehow.
     *
     * @param info the UriInfo instance of the current request
     * @param id the ID of a newly created entity under the base
     * @return the response builder with status 201 and location set to the entity with the provided id.
     */
    public static Response.ResponseBuilder created(UriInfo info,  String id) {
        return Response.status(Response.Status.CREATED).location(info.getRequestUriBuilder().path(id).build());
    }
}