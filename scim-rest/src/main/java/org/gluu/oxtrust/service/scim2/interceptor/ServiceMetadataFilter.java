/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */
package org.gluu.oxtrust.service.scim2.interceptor;

import static org.gluu.oxtrust.model.scim2.Constants.QUERY_PARAM_FILTER;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.gluu.oxtrust.model.scim2.ErrorScimType;
import org.gluu.oxtrust.ws.rs.scim2.BaseScimWebService;
import org.slf4j.Logger;

/**
 * This class checks whether a filter query parameter was provided, and if so, blocks the processing and returns an error
 * to the caller
 * Created by jgomer on 2017-11-27.
 */
@ApplicationScoped
@Provider
@RejectFilterParam
public class ServiceMetadataFilter implements ContainerRequestFilter {

    @Inject
    private Logger log;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        log.info("==== SCIM Service metadata call intercepted ====");
        String filter=requestContext.getUriInfo().getQueryParameters().getFirst(QUERY_PARAM_FILTER);
        if (filter!=null) {
            Response response=BaseScimWebService.getErrorResponse(Response.Status.FORBIDDEN, ErrorScimType.INVALID_VALUE, "No filter allowed here");
            requestContext.abortWith(response);
        }

    }

}