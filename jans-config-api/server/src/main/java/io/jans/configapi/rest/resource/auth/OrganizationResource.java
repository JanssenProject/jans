/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatchException;

import io.jans.as.persistence.model.GluuOrganization;
import io.jans.configapi.service.auth.OrganizationService;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.core.util.Jackson;

import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path(ApiConstants.ORG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrganizationResource extends BaseResource {

    @Inject
    OrganizationService organizationService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.ORG_CONFIG_READ_ACCESS })
    public Response getOrganization() {
        return Response.ok(organizationService.getOrganization()).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.ORG_CONFIG_WRITE_ACCESS })
    public Response patchOrganization(@NotNull String pathString) throws JsonPatchException, IOException {
        log.trace("Organization patch request - pathString:{} ", pathString);
        GluuOrganization organization = organizationService.getOrganization();
        organization = Jackson.applyPatch(pathString, organization);
        organizationService.updateOrganization(organization);
        return Response.ok(organizationService.getOrganization()).build();
    }

}