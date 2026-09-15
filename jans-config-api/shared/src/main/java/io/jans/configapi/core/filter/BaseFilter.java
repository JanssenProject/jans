/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

public abstract class BaseFilter implements ContainerRequestFilter {
    protected static final String AUTHENTICATION_SCHEME = "Bearer";

    protected void abortWithUnauthorized(ContainerRequestContext ctx, Response.Status status, String errMsg) {
        ctx.abortWith(Response.status(status).entity(errMsg)
                .header(HttpHeaders.WWW_AUTHENTICATE, AUTHENTICATION_SCHEME).build());
    }
}
