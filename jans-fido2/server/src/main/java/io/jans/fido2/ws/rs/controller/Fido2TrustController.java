/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ws.rs.controller;

import org.slf4j.Logger;

import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.model.trust.MdsHealth;
import io.jans.fido2.model.trust.MdsHealthStatus;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.trust.TrustStatusService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST API controller exposing attestation-mode and MDS health diagnostics.
 * <p>
 * Read-only: these endpoints surface configuration and metadata state that already exists in the
 * server, so an administrator can see why enrollments are failing instead of that appearing to end
 * users as a generic registration failure. Nothing here changes attestation behaviour, triggers a
 * metadata download, or touches the document store.
 * <p>
 * SECURITY NOTE: as with the metrics endpoints, authentication and authorization are expected to be
 * enforced at the infrastructure level (API gateway, OAuth interceptor, or reverse proxy) or via the
 * Config API fido2 plugin, which applies the fido2 configuration scopes.
 *
 * @author Janssen Project
 */
@ApplicationScoped
@Path("/trust")
public class Fido2TrustController {

    @Inject
    private Logger log;

    @Inject
    private TrustStatusService trustStatusService;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    /**
     * Effective attestation configuration: the mode in force, whether unattested authenticators are
     * still accepted, and whether the trust anchors attestation depends on are present.
     *
     * @return attestation trust configuration
     */
    @GET
    @Path("/attestation/config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAttestationConfig() {
        return processRequest(
                () -> Response.ok(dataMapperService.writeValueAsString(trustStatusService.getAttestationTrustConfig()))
                        .build());
    }

    /**
     * MDS health: loaded entry count, blob validity, and the outcome of the last refresh.
     * <p>
     * Answers 503 when the status is DOWN, so the endpoint can be wired straight to a monitor. A
     * metadata service switched off by configuration is reported as DISABLED with HTTP 200 — that is a
     * deliberate choice, not an outage.
     *
     * @return MDS health status
     */
    @GET
    @Path("/mds/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMdsHealth() {
        return processRequest(() -> {
            MdsHealth health = trustStatusService.getMdsHealth();
            String entity = dataMapperService.writeValueAsString(health);

            // The diagnostic body is returned with the 503 as well — a monitor that only sees the status
            // code still gets lastRefreshError and the entry count for free.
            Response.ResponseBuilder responseBuilder = MdsHealthStatus.DOWN == health.getStatus()
                    ? Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(entity)
                    : Response.ok(entity);

            return responseBuilder.build();
        });
    }

    private Response processRequest(RequestProcessor processor) {
        try {
            return processor.process();
        } catch (WebApplicationException e) {
            // Already carries the right status code.
            throw e;
        } catch (Exception e) {
            // Log the detail, return a generic message — the response must not leak internal paths or
            // connection details.
            log.error("Error processing trust status request: {}", e.getMessage(), e);
            throw errorResponseFactory.unknownError("Failed to read trust status");
        }
    }

    /**
     * Functional interface for request processing; throws Exception to accommodate the checked
     * exceptions raised while serializing the response.
     */
    private interface RequestProcessor {
        Response process() throws Exception;
    }
}
