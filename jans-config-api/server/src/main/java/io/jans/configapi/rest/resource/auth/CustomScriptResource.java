/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.service.custom.CustomScriptService;
import io.jans.util.StringHelper;

import com.github.fge.jsonpatch.JsonPatchException;

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

    /***
     * Method to fetch a custom scripts
     * 
     * @param type - type of the script
     * @throws NotAuthorizedException
     */
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_READ_ACCESS })
    public Response getAllCustomScripts() {
        List<CustomScript> customScripts = customScriptService.findAllCustomScripts(null);
        logger.debug("Custom Scripts:{}", customScripts);
        return Response.ok(customScripts).build();
    }
    
    /***
     * Method to fetch a custom script based on name
     * 
     * @param name - name of custom script
     * @throws NotAuthorizedException
     */
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

    /***
     * Method to fetch a custom script by type
     * 
     * @param type - type of the script
     * @return - List of CustomScript object
     * @throws NotAuthorizedException
     */
    @GET
    @Path(PATH_SEPARATOR + ApiConstants.TYPE + ApiConstants.TYPE_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_READ_ACCESS })
    public Response getCustomScriptsByTypePattern(@PathParam(ApiConstants.TYPE) @NotNull String type,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit) {
        
        if (logger.isDebugEnabled()) {
            logger.debug("Custom Script to be fetched based on type - type:{} , pattern:{}, limit:{} ", escapeLog(type), escapeLog(pattern), escapeLog(limit));
        }
        
        List<CustomScript> customScripts = this.customScriptService.findScriptByPatternAndType(pattern,
                CustomScriptType.getByValue(type.toLowerCase()), limit);
        logger.debug("Custom Scripts fetched :{}", customScripts);
        if (customScripts != null && !customScripts.isEmpty())
            return Response.ok(customScripts).build();
        else
            return Response.status(Response.Status.NOT_FOUND).build();
    }

    /***
     * Method to fetch a custom script by identifier - inum
     * 
     * @param inum - unique identifier of the script
     * @return - CustomScript object
     * @throws NotAuthorizedException
     */
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

    /***
     * Method to create a new custom script
     * 
     * @param customScript - CustomScript object
     * @return - CustomScript object
     * @throws NotAuthorizedException
     */
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

    /***
     * Method to update custom script
     * 
     * @param customScript - CustomScript object
     * @return - CustomScript object
     * @throws NotAuthorizedException
     * @throws NotFoundException
     */
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

    /**
     * Method to delete custom script
     * 
     * @param inum - unique identifier of the script
     * @throws NotAuthorizedException
     * @return
     */
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

    /***
     * Method to patch custom script
     * 
     * @param inum       - unique identifier of the script
     * @param pathString - A JSON Patch JSON file containing an array of patch
     *                   operations.
     * @return - CustomScript object
     * @throws NotAuthorizedException
     * @throws NotFoundException
     * @throws JsonPatchException
     * @throws IOException
     */
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

}
