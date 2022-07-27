/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.ClientService;
import io.jans.configapi.service.auth.UmaResourceService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
import io.jans.configapi.core.util.Jackson;
import io.jans.orm.exception.EntryPersistenceException;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 */

@Path(ApiConstants.UMA + ApiConstants.RESOURCES)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UmaResourcesResource extends ConfigBaseResource {

    private static final String UMA_RESOURCE = "Uma resource";

    @Inject
    UmaResourceService umaResourceService;

    @Inject
    ClientService clientService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.UMA_RESOURCES_READ_ACCESS })
    public Response fetchUmaResources(
            @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
        logger.debug("UMA_RESOURCE to be fetched - limit:{}, pattern:{}", limit, pattern);
        final List<UmaResource> resources;
        if (!pattern.isEmpty() && pattern.length() >= 2) {
            resources = umaResourceService.findResources(pattern, 1000);
        } else {
            resources = umaResourceService.getAllResources(limit);
        }
        return Response.ok(resources).build();
    }

    @GET
    @Path(ApiConstants.ID_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.UMA_RESOURCES_READ_ACCESS })
    public Response getUmaResourceByInum(@PathParam(value = ApiConstants.ID) @NotNull String id) {
        logger.debug("UMA_RESOURCE to fetch by id:{}", id);
        return Response.ok(findOrThrow(id)).build();
    }

    @GET
    @Path("/"+ApiConstants.CLIENTID + ApiConstants.CLIENTID_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.UMA_RESOURCES_READ_ACCESS })
    public Response getUmaResourceByAssociatedClient(
            @PathParam(value = ApiConstants.CLIENTID) @NotNull String associatedClientId) {
        logger.debug("UMA_RESOURCE to fetch by associatedClientId:{} ", associatedClientId);

        return Response.ok(getUmaResourceByClient(associatedClientId)).build();
    }

    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.UMA_RESOURCES_WRITE_ACCESS })
    public Response createUmaResource(@Valid UmaResource umaResource) {
        logger.debug("UMA_RESOURCE to be added umaResource:{}", umaResource);
        checkNotNull(umaResource.getName(), AttributeNames.NAME);
        checkNotNull(umaResource.getDescription(), AttributeNames.DESCRIPTION);
        String id = UUID.randomUUID().toString();
        umaResource.setId(id);
        umaResource.setDn(umaResourceService.getDnForResource(id));

        umaResourceService.addResource(umaResource);

        return Response.status(Response.Status.CREATED).entity(umaResource).build();
    }

    private UmaResource findOrThrow(String id) {
        try {
            UmaResource existingResource = umaResourceService.getResourceById(id);
            checkResourceNotNull(existingResource, UMA_RESOURCE);
            return existingResource;
        } catch (EntryPersistenceException e) {
            throw new NotFoundException(getNotFoundError(UMA_RESOURCE));
        }
    }

    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.UMA_RESOURCES_WRITE_ACCESS })
    public Response updateUmaResource(@Valid UmaResource resource) {
        logger.debug("UMA_RESOURCE to be upated - umaResource:{}", resource);
        String id = resource.getId();
        checkNotNull(id, AttributeNames.ID);
        UmaResource existingResource = findOrThrow(id);

        resource.setId(existingResource.getId());
        resource.setDn(umaResourceService.getDnForResource(id));
        umaResourceService.updateResource(resource);
        return Response.ok(resource).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.UMA_RESOURCES_WRITE_ACCESS })
    @Path(ApiConstants.ID_PATH)
    public Response patchResource(@PathParam(ApiConstants.ID) @NotNull String id, @NotNull String pathString)
            throws JsonPatchException, IOException {
        logger.debug("Patch for  id:{} , pathString:{}", id, pathString);
        UmaResource existingResource = findOrThrow(id);

        existingResource = Jackson.applyPatch(pathString, existingResource);
        umaResourceService.updateResource(existingResource);
        return Response.ok(existingResource).build();
    }

    @DELETE
    @Path(ApiConstants.ID_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.UMA_RESOURCES_DELETE_ACCESS })
    public Response deleteUmaResource(@PathParam(value = ApiConstants.ID) @NotNull String id) {
        logger.debug("UMA_RESOURCE to delete - id:{}", id);
        UmaResource umaResource = findOrThrow(id);
        umaResourceService.remove(umaResource);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private List<UmaResource> getUmaResourceByClient(String associatedClientId) {
        logger.debug("UMA RESOURCE to be fetched based on  associatedClientId:{}", associatedClientId);

        // Get client DN
        String associatedClientDn = this.clientService.getDnForClient(associatedClientId);
        logger.debug("UMA RESOURCE to be fetched based on  associatedClientId:{}", associatedClientId);

        return umaResourceService.getResourcesByClient(associatedClientDn);
    }
}
