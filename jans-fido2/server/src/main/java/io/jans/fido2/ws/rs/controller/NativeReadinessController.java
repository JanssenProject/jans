/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.ws.rs.controller;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.readiness.NativeReadinessResult;
import io.jans.fido2.service.readiness.NativeReadinessService;
import io.jans.fido2.service.readiness.NativeReadinessService.NotFound;
import io.jans.fido2.service.readiness.NativeReadinessService.RateLimited;
import jakarta.enterprise.context.ApplicationScoped;
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

/**
 * Runs the native app readiness checks for one configured relying party:
 * {@code POST /rp/{rpConfigId}/readiness}.
 * <p>
 * {@code rpConfigId} is the RP id exactly as configured in {@code requestedParties}, as a URL-encoded path
 * segment (an id such as {@code https://jans.io} contains {@code :} and {@code /}). It only selects a
 * configured entry. The body is empty or {@code {"recheck": true}}; any other field is ignored and never
 * read, so no request can name a host, URL, port or path to fetch.
 * <p>
 * Answers 200 with the structured result (also when readiness is disabled), 404 for an id that is not
 * configured, 429 with {@code Retry-After} when a rate limit applies. The response never carries fetched
 * content, response headers, resolved addresses or exception text.
 * <p>
 * SECURITY NOTE: as with {@link Fido2TrustController}, authentication and authorization are expected to be
 * enforced at the infrastructure level (API gateway, OAuth interceptor, or reverse proxy) or via the Config
 * API fido2 plugin, not here. The {@value #REQUESTED_BY_HEADER} header only labels the audit log. This
 * server cannot tell who sent it, so treat it as informational until the Config API hop that sets it is
 * trusted.
 *
 * @author Janssen Project
 */
@ApplicationScoped
@Path("/rp")
public class NativeReadinessController {

    static final String REQUESTED_BY_HEADER = "X-Jans-Requested-By";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    private Logger log;

    @Inject
    private NativeReadinessService readinessService;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @POST
    @Path("/{rpConfigId}/readiness")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response check(@PathParam("rpConfigId") String rpConfigId,
            @HeaderParam(REQUESTED_BY_HEADER) String requestedBy, String body) {
        boolean recheck = recheckRequested(body);
        try {
            NativeReadinessResult result = readinessService.check(rpConfigId, recheck, requestedBy);
            return Response.ok(dataMapperService.writeValueAsString(result)).build();
        } catch (NotFound e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (RateLimited e) {
            return Response.status(429).header("Retry-After", e.retryAfterSeconds()).build();
        } catch (Exception e) {
            // Log the detail, return a generic message: the response must not leak internals.
            log.error("Error processing native readiness request: {}", e.getMessage(), e);
            throw errorResponseFactory.unknownError("Failed to run native app readiness checks");
        }
    }

    private boolean recheckRequested(String body) {
        if (body == null || body.trim().isEmpty()) {
            return false;
        }
        try {
            JsonNode node = MAPPER.readTree(body);
            if (node == null || !node.isObject()) {
                throw errorResponseFactory.invalidRequest("The request body must be empty or a JSON object");
            }
            return node.path("recheck").asBoolean(false);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw errorResponseFactory.invalidRequest("The request body must be empty or a JSON object");
        }
    }
}
