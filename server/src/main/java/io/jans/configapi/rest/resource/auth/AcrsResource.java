/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.rest.model.AuthenticationMethod;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

/**
 * @author Puja Sharma
 */
@Path(ApiConstants.ACRS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AcrsResource extends BaseResource {

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.ACRS_READ_ACCESS })
    public Response getDefaultAuthenticationMethod() {
        final GluuConfiguration gluuConfiguration = configurationService.findGluuConfiguration();

        AuthenticationMethod authenticationMethod = new AuthenticationMethod();
        authenticationMethod.setDefaultAcr(gluuConfiguration.getAuthenticationMode());
        return Response.ok(authenticationMethod).build();
    }

    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.ACRS_WRITE_ACCESS })
    public Response updateDefaultAuthenticationMethod(@Valid AuthenticationMethod authenticationMethod) {
        log.debug("ACRS details to  update - authenticationMethod = "+authenticationMethod );
        final GluuConfiguration gluuConfiguration = configurationService.findGluuConfiguration();
        gluuConfiguration.setAuthenticationMode(authenticationMethod.getDefaultAcr());
        configurationService.merge(gluuConfiguration);
        return Response.ok(authenticationMethod).build();
    }

}