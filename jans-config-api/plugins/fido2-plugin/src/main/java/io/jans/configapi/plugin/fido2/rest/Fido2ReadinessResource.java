/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.fido2.rest;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.fido2.service.Fido2ReadinessService;
import io.jans.configapi.plugin.fido2.service.Fido2ReadinessService.Outcome;
import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * Runs the native app readiness check of the FIDO2 server for one configured relying party, behind Config
 * API authentication.
 * <p>
 * The call makes the FIDO2 server fetch the RP's {@code /.well-known} association files, so it needs a
 * write scope rather than a read one. The caller supplies only the configured RP id and, optionally,
 * {@code {"recheck": true}}; no URL, host, port or path is accepted.
 */
@Path(Constants.RP)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Fido2ReadinessResource extends BaseResource {

    private static final String ERR_MSG = "Exception while running Fido2 native app readiness check is - ";
    private static final String USER_INUM = "User-inum";

    @Inject
    Logger logger;

    @Inject
    Fido2ReadinessService fido2ReadinessService;

    /**
     * Run the readiness checks for one configured relying party.
     *
     * @param rpConfigId  the RP id exactly as configured, URL-encoded
     * @param token       the caller's Authorization header, forwarded to the FIDO2 server
     * @param requestedBy the authenticated user's inum when the caller supplies it, for the FIDO2 audit
     * @param body        empty or {@code {"recheck": true}}
     * @return the FIDO2 server's result, or its 404 / 429 (with {@code Retry-After})
     */
    @Operation(summary = "Run the native app readiness checks for a relying party.", description = "Checks that the relying party's assetlinks.json and apple-app-site-association files are reachable and match the configured native apps.", operationId = "post-fido2-native-readiness", tags = {
            "Fido2 - Native Readiness" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.FIDO2_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "No relying party is configured with this id"),
            @ApiResponse(responseCode = "429", description = "Too many checks; see the Retry-After header"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Path(Constants.RP_CONFIG_ID_READINESS)
    @ProtectedApi(scopes = { Constants.FIDO2_CONFIG_WRITE_ACCESS }, superScopes = { Constants.FIDO2_ADMIN_ACCESS,
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response checkReadiness(@PathParam("rpConfigId") String rpConfigId,
            @HeaderParam("Authorization") String token, @HeaderParam(USER_INUM) String requestedBy, String body) {
        logger.info("Fido2 native app readiness check requested");
        boolean recheck = recheckRequested(body);
        try {
            return toResponse(fido2ReadinessService.check(rpConfigId, recheck, token, requestedBy));
        } catch (WebApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error(ERR_MSG, ex);
            throwInternalServerException(ex);
        }
        return null;
    }

    /** 200, 400, 404 and 429 are passed on as the FIDO2 server sent them; anything else is a 500 here. */
    private Response toResponse(Outcome outcome) {
        int status = outcome.getStatus();
        if (status != Response.Status.OK.getStatusCode() && status != Response.Status.BAD_REQUEST.getStatusCode()
                && status != Response.Status.NOT_FOUND.getStatusCode() && status != 429) {
            logger.error("Fido2 native app readiness check failed upstream with status:{}", status);
            throwInternalServerException("The FIDO2 server could not run the readiness check");
        }
        Response.ResponseBuilder builder = Response.status(status);
        if (outcome.getBody() != null) {
            builder.entity(outcome.getBody());
        }
        if (status == 429 && StringUtils.isNotBlank(outcome.getRetryAfter())) {
            builder.header("Retry-After", outcome.getRetryAfter());
        }
        return builder.build();
    }

    private static boolean recheckRequested(String body) {
        if (StringUtils.isBlank(body)) {
            return false;
        }
        try {
            JsonNode node = Jackson.asJsonNode(body);
            if (node != null && node.isObject()) {
                return node.path("recheck").asBoolean(false);
            }
        } catch (Exception ex) {
            // falls through to the same rejection as a body that is not an object
        }
        throwBadRequestException("The request body must be empty or a JSON object");
        return false;
    }
}
