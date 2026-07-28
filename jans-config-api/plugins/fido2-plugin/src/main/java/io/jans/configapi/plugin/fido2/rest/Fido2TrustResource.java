/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.fido2.rest;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.fido2.service.Fido2TrustService;
import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

/**
 * Read-only attestation and MDS diagnostics, surfaced through the Config API for the Admin UI.
 * <p>
 * GitHub Issue #14602
 */
@Path(Constants.TRUST)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Fido2TrustResource extends BaseResource {

    private static final String ERR_MSG = "Exception while getting Fido2 trust data is - ";

    @Inject
    Logger logger;

    @Inject
    Fido2TrustService fido2TrustService;

    /**
     * Retrieve the attestation policy the FIDO2 server is actually applying.
     *
     * @return a Response containing a JsonNode with the effective attestation configuration
     */
    @Operation(summary = "Get effective Fido2 attestation configuration.", description = "Get effective Fido2 attestation configuration.", operationId = "get-fido2-trust-attestation-config", tags = {
            "Fido2 - Trust" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/trust/fido2-attestation-config.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(Constants.ATTESTATION_CONFIG)
    @ProtectedApi(scopes = { Constants.FIDO2_CONFIG_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getAttestationConfig() {
        logger.info("Fido2 attestation trust configuration");
        JsonNode jsonNode = null;
        try {
            jsonNode = fido2TrustService.getAttestationConfig(null);

            if (logger.isDebugEnabled()) {
                logger.debug("Fido2 attestation trust configuration - jsonNode:{}", jsonNode);
            }

        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }
        return Response.ok(jsonNode).build();
    }

}
