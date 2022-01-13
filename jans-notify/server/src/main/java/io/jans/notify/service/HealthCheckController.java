/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.service;

import javax.enterprise.context.ApplicationScoped;
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

    @GET
    @POST
    @Path("/health-check")
    @Produces(MediaType.APPLICATION_JSON)
	public String healthCheckController() {
        return "{\"status\":\"running\"}";
	}

}
