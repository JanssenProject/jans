/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.ssa.ws.rs;

import io.jans.as.server.ssa.ws.rs.action.SsaCreateAction;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/")
public class SsaRestWebServiceImpl implements SsaRestWebService {

    @Inject
    private SsaCreateAction ssaCreateAction;

    @Override
    public Response create(String requestParams, HttpServletRequest httpRequest, SecurityContext securityContext) {
        return ssaCreateAction.create(requestParams, httpRequest, securityContext);
    }
}