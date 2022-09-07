/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs.controller;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.orm.PersistenceEntryManager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Health check controller
 *
 * @author Yuriy Movchan
 * @version Jul 24, 2020
 */
@ApplicationScoped
@Path("/")
public class HealthCheckController {

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @GET
    @POST
    @Path("/health-check")
    @Produces(MediaType.APPLICATION_JSON)
    public String healthCheckController() {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.HEALTH_CHECK);
        boolean isConnected = persistenceEntryManager.getOperationService().isConnected();
        String dbStatus = isConnected ? "online" : "offline";
        return "{\"status\": \"running\", \"db_status\":\"" + dbStatus + "\"}";
    }

}
