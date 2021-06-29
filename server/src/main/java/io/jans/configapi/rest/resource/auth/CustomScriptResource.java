/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.service.custom.CustomScriptService;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Path(ApiConstants.CONFIG + ApiConstants.SCRIPTS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CustomScriptResource extends BaseResource {

    private static final String CUSTOM_SCRIPT = "custom script";
    private static final String PATH_SEPARATOR = "/";

    @Inject
    Logger log;

    @Inject
    CustomScriptService customScriptService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_READ_ACCESS })
    public Response getAllCustomScripts() {
        List<CustomScript> customScripts = customScriptService.findAllCustomScripts(null);
        return Response.ok(customScripts).build();
    }

    @GET
    @Path(PATH_SEPARATOR + ApiConstants.TYPE + ApiConstants.TYPE_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_READ_ACCESS })
    public Response getCustomScriptsByTypePattern(@PathParam(ApiConstants.TYPE) @NotNull String type,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit) {
        List<CustomScript> customScripts = this.customScriptService.findScriptByPatternAndType(pattern,
                CustomScriptType.getByValue(type.toLowerCase()), limit);
        if (customScripts != null && !customScripts.isEmpty())
            return Response.ok(customScripts).build();
        else
            return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path(PATH_SEPARATOR + ApiConstants.INUM + PATH_SEPARATOR + ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_READ_ACCESS })
    public Response getCustomScriptByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        log.debug("CustomScript to be fetched - inum = " + inum);
        CustomScript script = null;
        try {
            script = this.customScriptService.getScriptByInum(inum);
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex.getMessage().contains("Failed to find entry")) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        }
        return Response.ok(script).build();
    }

    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_WRITE_ACCESS })
    public Response createScript(@Valid CustomScript customScript) {
        log.debug("CustomScriptResource::createScript() - customScript = " + customScript + "\n\n");
        Objects.requireNonNull(customScript, "Attempt to create null custom script");
        String inum = customScript.getInum();
        if (StringHelper.isEmpty(inum)) {
            inum = UUID.randomUUID().toString();
        }
        customScript.setDn(customScriptService.buildDn(inum));
        customScript.setInum(inum);
        customScriptService.add(customScript);
        return Response.status(Response.Status.CREATED).entity(customScript).build();
    }

    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_WRITE_ACCESS })
    public Response updateScript(@Valid @NotNull CustomScript customScript) {
        log.debug("CustomScriptResource::updateScript() - customScript = " + customScript + "\n\n");
        CustomScript existingScript = customScriptService.getScriptByInum(customScript.getInum());
        checkResourceNotNull(existingScript, CUSTOM_SCRIPT);
        customScript.setInum(existingScript.getInum());
        customScriptService.update(customScript);
        return Response.ok(customScript).build();
    }

    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.SCRIPTS_DELETE_ACCESS })
    public Response deleteScript(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        try {
            log.debug("CustomScriptResource::deleteScript() - inum = " + inum + "\n\n");
            CustomScript existingScript = customScriptService.getScriptByInum(inum);
            customScriptService.remove(existingScript);
            return Response.noContent().build();
        } catch (Exception ex) {
            log.info("Error deleting script by inum " + inum, ex);
            throw new NotFoundException(getNotFoundError(CUSTOM_SCRIPT));
        }
    }

}
