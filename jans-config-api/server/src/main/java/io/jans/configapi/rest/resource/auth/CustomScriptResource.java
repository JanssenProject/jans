/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.model.SearchRequest;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.GluuAttribute;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.orm.model.PagedResult;
import io.jans.service.custom.CustomScriptService;
import io.jans.util.StringHelper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
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
import java.util.Objects;
import java.util.UUID;

@Path(ApiConstants.CONFIG + ApiConstants.SCRIPTS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CustomScriptResource extends ConfigBaseResource {

    private static final String CUSTOM_SCRIPT = "custom script";
    private static final String PATH_SEPARATOR = "/";

    @Inject
    CustomScriptService customScriptService;

    @Operation(summary = "Fetch custom script by name", description = "Gets a list of custom scripts", operationId = "get-config-scripts", tags = {
            "Custom Scripts" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCRIPTS_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScript.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_READ_ACCESS })
    public Response getAllCustomScripts( @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(ApiConstants.ALL) @QueryParam(value = ApiConstants.STATUS) String status,
            @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder) {
        
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Search Custom Script filters with limit:{}, pattern:{}, status:{}, startIndex:{}, sortBy:{}, sortOrder:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(status), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder));
        }
        
        List<CustomScript> customScripts = customScriptService.findAllCustomScripts(null);
        logger.debug("Custom Scripts:{}", customScripts);
        return Response.ok(customScripts).build();
    }

    @Operation(summary = "Fetch custom script by name", description = "Fetch custom script by name", operationId = "get-custom-script-by-name", tags = {
            "Custom Scripts" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCRIPTS_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CustomScript", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScript.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(PATH_SEPARATOR + ApiConstants.NAME + ApiConstants.NAME_PARAM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_READ_ACCESS })
    public Response getCustomScriptByName(@PathParam(ApiConstants.NAME) @NotNull String name) {

        if (logger.isDebugEnabled()) {
            logger.debug("Custom Script to be fetched based on type - name:{} ", escapeLog(name));
        }

        CustomScript customScript = customScriptService.getScriptByDisplayName(name);
        checkResourceNotNull(customScript, CUSTOM_SCRIPT);

        logger.debug("Custom Script Fetched based on name:{}, customScript:{}", name, customScript);
        return Response.ok(customScript).build();
    }

    @Operation(summary = "Gets list of scripts by type", description = "Gets list of scripts by type", operationId = "get-config-scripts-by-type", tags = {
            "Custom Scripts" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCRIPTS_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = CustomScript.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(PATH_SEPARATOR + ApiConstants.TYPE + ApiConstants.TYPE_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_READ_ACCESS })
    public Response getCustomScriptsByTypePattern(@PathParam(ApiConstants.TYPE) @NotNull String type,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit) {

        if (logger.isDebugEnabled()) {
            logger.debug("Custom Script to be fetched based on type - type:{} , pattern:{}, limit:{} ", escapeLog(type),
                    escapeLog(pattern), escapeLog(limit));
        }

        List<CustomScript> customScripts = this.customScriptService.findScriptByPatternAndType(pattern,
                CustomScriptType.getByValue(type.toLowerCase()), limit);
        logger.debug("Custom Scripts fetched :{}", customScripts);
        if (customScripts != null && !customScripts.isEmpty())
            return Response.ok(customScripts).build();
        else
            return Response.status(Response.Status.NOT_FOUND).build();
    }

    @Operation(summary = "Gets a script by Inum", description = "Gets a script by Inum", operationId = "get-config-scripts-by-inum", tags = {
            "Custom Scripts" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCRIPTS_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScript.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(PATH_SEPARATOR + ApiConstants.INUM + PATH_SEPARATOR + ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_READ_ACCESS })
    public Response getCustomScriptByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (logger.isDebugEnabled()) {
            logger.debug("Custom Script to be fetched - inum:{} ", escapeLog(inum));
        }
        CustomScript script = null;
        try {
            script = this.customScriptService.getScriptByInum(inum);
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex.getMessage().contains("Failed to find entry")) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        }
        logger.debug("Custom Script fetched by inum :{}", script);
        return Response.ok(script).build();
    }

    @Operation(summary = "Adds a new custom script", description = "Adds a new custom script", operationId = "post-config-scripts", tags = {
            "Custom Scripts" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCRIPTS_WRITE_ACCESS }))
    @RequestBody(description = "CustomScript object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScript.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScript.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_WRITE_ACCESS })
    public Response createScript(@Valid CustomScript customScript) {
        logger.debug("Custom Script to create - customScript:{}", customScript);
        Objects.requireNonNull(customScript, "Attempt to create null custom script");
        String inum = customScript.getInum();
        if (StringHelper.isEmpty(inum)) {
            inum = UUID.randomUUID().toString();
        }
        customScript.setDn(customScriptService.buildDn(inum));
        customScript.setInum(inum);
        customScriptService.add(customScript);
        logger.debug("Custom Script added {}", customScript);
        return Response.status(Response.Status.CREATED).entity(customScript).build();
    }

    @Operation(summary = "Updates a custom script", description = "Updates a custom script", operationId = "put-config-scripts", tags = {
            "Custom Scripts" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCRIPTS_WRITE_ACCESS }))
    @RequestBody(description = "CustomScript object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScript.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScript.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_WRITE_ACCESS })
    public Response updateScript(@Valid @NotNull CustomScript customScript) {
        logger.debug("Custom Script to update - customScript:{}", customScript);
        CustomScript existingScript = customScriptService.getScriptByInum(customScript.getInum());
        checkResourceNotNull(existingScript, CUSTOM_SCRIPT);
        customScript.setInum(existingScript.getInum());
        logger.debug("Custom Script updated {}", customScript);
        customScriptService.update(customScript);
        return Response.ok(customScript).build();
    }

    @Operation(summary = "Deletes a custom script", description = "Deletes a custom script", operationId = "delete-config-scripts-by-inum", tags = {
            "Custom Scripts" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCRIPTS_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_DELETE_ACCESS })
    public Response deleteScript(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Custom Script Resource to delete - inum:{}", escapeLog(inum));
            }
            CustomScript existingScript = customScriptService.getScriptByInum(inum);
            customScriptService.remove(existingScript);
            return Response.noContent().build();
        } catch (Exception ex) {
            logger.info("Error deleting script by inum " + inum, ex);
            throw new NotFoundException(getNotFoundError(CUSTOM_SCRIPT));
        }
    }

    @Operation(summary = "Patches a custom script", description = "Patches a custom script", operationId = "patch-config-scripts-by-inum", tags = {
            "Custom Scripts" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCRIPTS_WRITE_ACCESS }))
    @RequestBody(description = "JsonPatch object", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScript.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchAtribute(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString)
            throws JsonPatchException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Custom Script Resource to patch - inum:{} , pathString:{}", escapeLog(inum),
                    escapeLog(pathString));
        }

        CustomScript existingScript = customScriptService.getScriptByInum(inum);
        checkResourceNotNull(existingScript, CUSTOM_SCRIPT);
        existingScript = Jackson.applyPatch(pathString, existingScript);
        customScriptService.update(existingScript);
        existingScript = customScriptService.getScriptByInum(inum);

        logger.debug(" Custom Script Resource after patch - existingScript:{}", existingScript);
        return Response.ok(existingScript).build();
    }
    
    private PagedResult<CustomScript> doSearch(SearchRequest searchReq, CustomScriptType type) {

        logger.debug("GluuAttribute search params - searchReq:{} , type:{} ", searchReq, type);

        PagedResult<CustomScript> pagedResult = customScriptService.searchFlows(searchReq, type);

        logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        if (pagedResult != null) {
            logger.debug(
                    "GluuAttributes fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());
        }

        logger.debug("GluuAttributes fetched new  - pagedResult:{} ", pagedResult);
        return pagedResult;
    }


}
