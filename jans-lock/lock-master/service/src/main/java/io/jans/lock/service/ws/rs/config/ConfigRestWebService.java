/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.lock.service.ws.rs.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Provides interface for configuration REST web services
 *
 * @author Yuriy Movchan Date: 06/06/2024
 */
public interface ConfigRestWebService {

	@GET
	@Path("/config")
	@Produces({ MediaType.APPLICATION_JSON })
	Response processConfigRequest(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context SecurityContext sec);

	@GET
	@Path("/config/issuers")
	@Produces({ MediaType.APPLICATION_JSON })
	Response processIssuersRequest(@Context HttpServletRequest request, @Context HttpServletResponse response,
			@Context SecurityContext sec);

	@GET
	@Path("/config/policy")
	@Produces({ MediaType.APPLICATION_JSON })
	Response processPolicyRequest(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context SecurityContext sec);

	@GET
	@Path("​/config​/schema")
	@Produces({ MediaType.APPLICATION_JSON })
	Response processSchemaRequest(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context SecurityContext sec);

}