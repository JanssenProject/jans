/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.configapi.service.auth.OrganizationService;
import io.jans.as.persistence.model.GluuOrganization;
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
@Path(ApiConstants.ORG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrganizationResource extends BaseResource {

    @Inject
    Logger log;

    @Inject
    OrganizationService organizationService;

    @GET
    //@ProtectedApi(scopes = { ApiAccessConstants.ACRS_READ_ACCESS })
    public Response getDefaultAuthenticationMethod() {
        final GluuOrganization gluuOrganization = organizationService.getOrganization();
        log.error("\n\n OrganizationResource::gluuOrganization() - gluuOrganization:{} ",gluuOrganization);
     
        return Response.ok(gluuOrganization).build();
    }

 
}