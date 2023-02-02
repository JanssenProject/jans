/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.service.custom.CustomScriptService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.ScriptLocationType;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.orm.model.PagedResult;
import io.jans.util.StringHelper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

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
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/scripts/scripts-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.SCRIPTS_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getAllCustomScripts(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder) {

        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Search Custom Script filters with limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder));
        }

        return Response.ok(doSearch(pattern, sortBy, sortOrder, startIndex, limit, this.getMaxCount(), null)).build();
    }

    @Operation(summary = "Fetch custom script by name", description = "Fetch custom script by name", operationId = "get-custom-script-by-name", tags = {
            "Custom Scripts" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCRIPTS_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CustomScript", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScript.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/scripts/scripts-by-name.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(PATH_SEPARATOR + ApiConstants.NAME + ApiConstants.NAME_PARAM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.SCRIPTS_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getCustomScriptByName(
            @Parameter(description = "Script name") @PathParam(ApiConstants.NAME) @NotNull String name) {

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
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/scripts/scripts-by-type.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(PATH_SEPARATOR + ApiConstants.TYPE + ApiConstants.TYPE_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.SCRIPTS_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getCustomScriptsByTypePattern(
            @Parameter(description = "Script type") @PathParam(ApiConstants.TYPE) @NotNull String type,
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder) {

        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Custom Script to be fetched based on type - type:{}, limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}",
                    escapeLog(type), escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder));
        }

        return Response.ok(doSearch(pattern, sortBy, sortOrder, startIndex, limit, this.getMaxCount(),
                CustomScriptType.getByValue(type.toLowerCase()))).build();
    }

    @Operation(summary = "Gets a script by Inum", description = "Gets a script by Inum", operationId = "get-config-scripts-by-inum", tags = {
            "Custom Scripts" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCRIPTS_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScript.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/scripts/scripts-by-id.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(PATH_SEPARATOR + ApiConstants.INUM + PATH_SEPARATOR + ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.SCRIPTS_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getCustomScriptByInum(
            @Parameter(description = "Script identifier") @PathParam(ApiConstants.INUM) @NotNull String inum) {
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
    @RequestBody(description = "CustomScript object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScript.class), examples = @ExampleObject(name = "Request json example", value = "example/auth/scripts/scripts.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScript.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/scripts/scripts-response.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response createScript(@Valid CustomScript customScript,
            @Parameter(description = "Boolean flag to indicate if script template is to be added. If CustomScript request object has script populated then script template will not be added.") @DefaultValue("false") @QueryParam(value = ApiConstants.ADD_SCRIPT_TEMPLATE) boolean addScriptTemplate) {
        logger.info("Custom Script to create - customScript:{}, addScriptTemplate:{}", customScript, addScriptTemplate);
        Objects.requireNonNull(customScript, "Attempt to create null custom script");
        String inum = customScript.getInum();
        if (StringHelper.isEmpty(inum)) {
            inum = UUID.randomUUID().toString();
        }
        if (StringUtils.isBlank(customScript.getScript()) && !addScriptTemplate) {
            customScript.setScript(""); // this will ensure that default Script Template is not added
        }

        // validate Script LocationType value
        validateScriptLocationType(customScript);

        customScript.setDn(customScriptService.buildDn(inum));
        customScript.setInum(inum);
        customScriptService.add(customScript);
        logger.debug("Custom Script added {}", customScript);
        return Response.status(Response.Status.CREATED).entity(customScript).build();
    }

    @Operation(summary = "Updates a custom script", description = "Updates a custom script", operationId = "put-config-scripts", tags = {
            "Custom Scripts" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCRIPTS_WRITE_ACCESS }))
    @RequestBody(description = "CustomScript object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScript.class), examples = @ExampleObject(name = "Request json example", value = "example/auth/scripts/scripts.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScript.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/scripts/scripts-response.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateScript(@Valid @NotNull CustomScript customScript) {
        logger.debug("Custom Script to update - customScript:{}", customScript);
        CustomScript existingScript = customScriptService.getScriptByInum(customScript.getInum());
        checkResourceNotNull(existingScript, CUSTOM_SCRIPT);
        customScript.setInum(existingScript.getInum());
        logger.debug("Custom Script updated {}", customScript);

        // validate Script LocationType value
        validateScriptLocationType(customScript);

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
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_DELETE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    public Response deleteScript(
            @Parameter(description = "Script identifier") @PathParam(ApiConstants.INUM) @NotNull String inum) {
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
    @RequestBody(description = "JsonPatch object", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/auth/scripts/scripts-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScript.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchScript(
            @Parameter(description = "Script identifier") @PathParam(ApiConstants.INUM) @NotNull String inum,
            @NotNull String pathString) throws JsonPatchException, IOException {
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

    private PagedResult<CustomScript> doSearch(String pattern, String sortBy, String sortOrder, Integer startIndex,
            int limit, int maximumRecCount, CustomScriptType type) {

        logger.debug(
                "CustomScript search params -  - pattern:{}, sortBy:{}, sortOrder:{}, startIndex:{}, limit:{}, maximumRecCount:{}, type:{}",
                pattern, sortBy, sortOrder, startIndex, limit, maximumRecCount, type);

        PagedResult<CustomScript> pagedResult = customScriptService.searchScripts(pattern, sortBy, sortOrder,
                startIndex, limit, maximumRecCount, type);

        logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        if (pagedResult != null) {
            logger.debug(
                    "CustomScripts fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());
        }

        logger.debug("CustomScript pagedResult:{} ", pagedResult);
        return pagedResult;
    }

    /**
     * ScriptLocationType.LDAP has been deprecated and hence for any new script
     * creation or modification we need to ensure that valid values are used.
     * 
     * @param customScript
     */
    private void validateScriptLocationType(CustomScript customScript) {
        logger.info("validate ScriptLocationType - customScript:{}", customScript);
        if (customScript == null || customScript.getLocationType() == null) {
            return;
        }

        if (ScriptLocationType.LDAP.getValue().equalsIgnoreCase(customScript.getLocationType().getValue())) {
            ScriptLocationType[] types = ScriptLocationType.values();
            StringBuilder scriptLocationType = new StringBuilder();
            if (types != null) {
                for (ScriptLocationType type : types) {
                    scriptLocationType.append(type.getValue());
                    scriptLocationType.append(",");
                }
                scriptLocationType.delete(scriptLocationType.lastIndexOf(","), scriptLocationType.lastIndexOf(",") + 1);
            }

            throwBadRequestException("Invalid value for script location Type in request -> "
                    + customScript.getLocationType().getValue() + " , valid values are " + scriptLocationType);
        }
    }

}
