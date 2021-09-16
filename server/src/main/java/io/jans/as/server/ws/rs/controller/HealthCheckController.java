/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs.controller;

import io.jans.as.model.common.ComponentType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.orm.PersistenceEntryManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
        errorResponseFactory.validateComponentEnabled(ComponentType.HEALTH_CHECK);
        boolean isConnected = persistenceEntryManager.getOperationService().isConnected();
        String dbStatus = isConnected ? "online" : "offline";
        return "{\"status\": \"running\", \"db_status\":\"" + dbStatus + "\"}";
    }

}
