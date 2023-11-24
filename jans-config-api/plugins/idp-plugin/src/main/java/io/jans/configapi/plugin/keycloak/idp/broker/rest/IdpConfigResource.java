/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.rest;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.keycloak.idp.broker.model.config.IdpAppConfiguration;
import io.jans.configapi.plugin.keycloak.idp.broker.model.config.IdpConf;
import io.jans.configapi.plugin.keycloak.idp.broker.service.IdpConfigService;
import io.jans.configapi.plugin.keycloak.idp.broker.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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

import org.slf4j.Logger;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

@Path(Constants.IDP_CONFIG_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IdpConfigResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    IdpConfigService idpConfigService;

    @Operation(summary = "Gets IDP configuration properties", description = "Gets IDP configuration properties", operationId = "get-idp-config-properties", tags = {
            "Jans - SAML IDP Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_CONFIG_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdpAppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.JANS_IDP_CONFIG_READ_ACCESS }, groupScopes = {
            Constants.JANS_IDP_CONFIG_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getIdpConfiguration() {
        IdpAppConfiguration idpAppConfiguration = idpConfigService.getIdpAppConfiguration();
        logger.info("IDP idpAppConfiguration():{}", idpAppConfiguration);
        return Response.ok(idpAppConfiguration).build();
    }

    @Operation(summary = "Update IDP configuration properties", description = "Update IDP configuration properties", operationId = "put-idp-properties", tags = {
            "Jans - SAML IDP Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_CONFIG_WRITE_ACCESS }))
    @RequestBody(description = "GluuAttribute object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdpAppConfiguration.class), examples = @ExampleObject(name = "Request example", value = "example/idp/config/idp-config-put.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdpAppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT    @ProtectedApi(scopes = { Constants.JANS_IDP_CONFIG_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateIdpConfiguration(@Valid IdpAppConfiguration idpAppConfiguration) {
        logger.info("Update IDP details idpAppConfiguration():{}", idpAppConfiguration);
        IdpConf conf = idpConfigService.findIdpConf();
        conf.setDynamicConf(idpAppConfiguration);
        idpConfigService.mergeIdpConfig(conf);
        idpAppConfiguration = idpConfigService.getIdpAppConfiguration();
        logger.info("IDP post update - idpAppConfiguration:{}", idpAppConfiguration);
        return Response.ok(idpAppConfiguration).build();

    }

    @Operation(summary = "Partially modifies IDP configuration properties.", description = "Partially modifies IDP Configuration properties.", operationId = "patch-idp-properties", tags = {
            "Jans - SAML IDP Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_CONFIG_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/dp/config/idp-config-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdpAppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { Constants.JANS_IDP_CONFIG_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchIdpConfiguration(@NotNull String jsonPatchString) throws JsonPatchException, IOException {
        logger.info("IDP Config patch  - jsonPatchString:{} ", jsonPatchString);
        IdpConf conf = idpConfigService.findIdpConf();
        IdpAppConfiguration idpAppConfiguration = Jackson.applyPatch(jsonPatchString, conf.getDynamicConf());
        conf.setDynamicConf(idpAppConfiguration);
        idpConfigService.mergeIdpConfig(conf);
        idpAppConfiguration = idpConfigService.getIdpAppConfiguration();
        logger.info("IDP config post patch - idpAppConfiguration:{}", idpAppConfiguration);
        return Response.ok(idpAppConfiguration).build();
    }
}