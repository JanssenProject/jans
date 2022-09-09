/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatch;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

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

    @Operation(summary = "Gets list of UMA resources", description = "Gets list of UMA resources", operationId = "get-oauth-uma-resources", tags = {
            "OAuth - UMA Resources" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.UMA_RESOURCES_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = UmaResource.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.UMA_RESOURCES_READ_ACCESS })
    public Response fetchUmaResources(
            @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
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

    @Operation(summary = "Gets an UMA resource by ID", description = "Gets an UMA resource by ID", operationId = "get-oauth-uma-resources-by-id", tags = {
            "OAuth - UMA Resources" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.UMA_RESOURCES_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UmaResource.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ApiConstants.ID_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.UMA_RESOURCES_READ_ACCESS })
    public Response getUmaResourceByInum(@PathParam(value = ApiConstants.ID) @NotNull String id) {
        logger.debug("UMA_RESOURCE to fetch by id:{}", id);
        return Response.ok(findOrThrow(id)).build();
    }

    @Operation(summary = "Fetch uma resources by client id", description = "Fetch uma resources by client id", operationId = "get-oauth-uma-resources-by-clientid", tags = {
            "OAuth - UMA Resources" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.UMA_RESOURCES_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = UmaResource.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/" + ApiConstants.CLIENTID + ApiConstants.CLIENTID_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.UMA_RESOURCES_READ_ACCESS })
    public Response getUmaResourceByAssociatedClient(
            @PathParam(value = ApiConstants.CLIENTID) @NotNull String associatedClientId) {
        logger.debug("UMA_RESOURCE to fetch by associatedClientId:{} ", associatedClientId);

        return Response.ok(getUmaResourceByClient(associatedClientId)).build();
    }

    @Operation(summary = "Creates an UMA resource", description = "Creates an UMA resource", operationId = "post-oauth-uma-resources", tags = {
            "OAuth - UMA Resources" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.UMA_RESOURCES_WRITE_ACCESS }))
    @RequestBody(description = "UmaResource object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UmaResource.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UmaResource.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
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

    @Operation(summary = "Updates an UMA resource", description = "Updates an UMA resource", operationId = "put-oauth-uma-resources", tags = {
            "OAuth - UMA Resources" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.UMA_RESOURCES_WRITE_ACCESS }))
    @RequestBody(description = "UmaResource object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UmaResource.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "UmaResource", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UmaResource.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
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

    @Operation(summary = "Patch UMA resource", description = "Patch UMA resource", operationId = "patch-oauth-uma-resources-by-id", tags = {
            "OAuth - UMA Resources" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.UMA_RESOURCES_WRITE_ACCESS  }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = {
            @ExampleObject(value = "[ {op:replace, path: clients, value: [\"client_1\",\"client_2\"] },{op:add, path: clients/2, value: \"client_3\" } ]") }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UmaResource.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
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

    @Operation(summary = "Deletes an UMA resource", description = "Deletes an UMA resource", operationId = "delete-oauth-uma-resources-by-id", tags = {
            "OAuth - UMA Resources" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.UMA_RESOURCES_DELETE_ACCESS  }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
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
