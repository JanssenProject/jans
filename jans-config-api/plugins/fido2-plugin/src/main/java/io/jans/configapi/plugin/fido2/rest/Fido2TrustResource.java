/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.fido2.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.fido2.service.Fido2TrustService;
import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.plugin.fido2.util.Fido2Util;
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

    @Inject
    Fido2Util fido2Util;

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

    /**
     * Retrieve the health of the metadata the FIDO2 server uses for attestation validation.
     *
     * @return a Response containing a JsonNode with the MDS health status
     */
    @Operation(summary = "Get Fido2 MDS health.", description = "Get Fido2 MDS health.", operationId = "get-fido2-trust-mds-health", tags = {
            "Fido2 - Trust" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response example", value = "example/fido2/trust/fido2-mds-health.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError"),
            @ApiResponse(responseCode = "503", description = "Service Unavailable - the metadata service is DOWN", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class))) })
    @GET
    @Path(Constants.MDS_HEALTH)
    @ProtectedApi(scopes = { Constants.FIDO2_CONFIG_READ_ACCESS }, groupScopes = {
            Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getMdsHealth() {
        logger.info("Fido2 MDS health");
        JsonNode jsonNode = null;
        try {
            jsonNode = fido2TrustService.getMdsHealth(null);

            if (logger.isDebugEnabled()) {
                logger.debug("Fido2 MDS health - jsonNode:{}", jsonNode);
            }

        } catch (WebApplicationException ex) {
            // The FIDO2 server answers 503 with a diagnostic body when MDS is DOWN, so that the endpoint
            // can be wired straight to a monitor. Fido2Util turns any non-OK upstream status into a
            // WebApplicationException carrying that body as its message; flattening it to 500 here would
            // lose both the DOWN signal and the reason, and a monitor pointed at the Config API would
            // then disagree with one pointed at the FIDO2 server. Mirror the 503 instead.
            Response mirrored = mirrorServiceUnavailable(ex);
            if (mirrored != null) {
                return mirrored;
            }
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }
        return Response.ok(jsonNode).build();
    }

    /**
     * Rebuilds an upstream 503 as a 503 from this endpoint, preserving the MDS health body.
     *
     * @return the mirrored response, or {@code null} when the failure was not an upstream 503, in which
     *         case the caller falls back to the usual 500 handling
     */
    private Response mirrorServiceUnavailable(WebApplicationException ex) {
        if (ex.getResponse() == null
                || ex.getResponse().getStatus() != Response.Status.SERVICE_UNAVAILABLE.getStatusCode()) {
            return null;
        }
        try {
            JsonNode body = fido2Util.getResponseJsonNode(ex.getMessage(), null);
            logger.warn("Fido2 MDS health reported DOWN by the Fido2 server");
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(body).build();
        } catch (JsonProcessingException jpe) {
            logger.error("Unable to parse the MDS health body returned with the upstream 503", jpe);
            return null;
        }
    }
}
