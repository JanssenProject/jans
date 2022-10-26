package io.jans.configapi.plugin.scim.rest;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.scim.service.ScimConfigService;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.scim.util.Constants;
import io.jans.scim.model.conf.AppConfiguration;
import io.jans.scim.model.conf.Conf;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import org.slf4j.Logger;

import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path(Constants.SCIM_CONFIG)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScimConfigResource {

    @Inject
    Logger log;

    @Inject
    ScimConfigService scimConfigService;

    @Operation(summary = "Retrieves SCIM App configuration", description = "Retrieves SCIM App configuration", operationId = "get-scim-config", tags = {
            "SCIM - Config Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    "https://jans.io/scim/config.readonly" }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { "https://jans.io/scim/config.readonly" })
    public Response getAppConfiguration() {
        AppConfiguration appConfiguration = scimConfigService.find();
        log.debug("SCIM appConfiguration:{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }

    @Operation(summary = "Patch SCIM App configuration", description = "Patch SCIM App configuration", operationId = "patch-scim-config", tags = {
            "SCIM - Config Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    "https://jans.io/scim/config.write" }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = {
            @ExampleObject(value = "[ {op:replace, path: loggingLevel, value: DEBUG } ]") }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { "https://jans.io/scim/config.write" })
    public Response patchAppConfigurationProperty(@NotNull String requestString)
            throws IOException, JsonPatchException {
        log.debug("AUTH CONF details to patch - requestString:{}", requestString);
        Conf conf = scimConfigService.findConf();
        AppConfiguration appConfiguration = conf.getDynamicConf();
        log.trace("AUTH CONF details BEFORE patch - conf:{}, appConfiguration:{}", conf, appConfiguration);

        appConfiguration = Jackson.applyPatch(requestString, appConfiguration);
        log.trace("AUTH CONF details BEFORE patch merge - appConfiguration:{}", appConfiguration);
        conf.setDynamicConf(appConfiguration);

        scimConfigService.merge(conf);
        appConfiguration = scimConfigService.find();
        log.debug("AUTH CONF details AFTER patch merge - appConfiguration:{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }

}
