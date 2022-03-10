/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.ws.rs.scim2;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.jans.orm.PersistenceEntryManager;

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

    @GET
    @POST
    @Path("/health-check")
    @Produces(MediaType.APPLICATION_JSON)
	public String healthCheckController() {
    	boolean isConnected = persistenceEntryManager.getOperationService().isConnected();
    	String dbStatus = isConnected ? "online" : "offline"; 
        return "{\"status\": \"running\", \"db_status\":\"" + dbStatus + "\"}";
	}

}
