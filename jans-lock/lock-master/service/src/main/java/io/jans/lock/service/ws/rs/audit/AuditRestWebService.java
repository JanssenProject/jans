/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.lock.service.ws.rs.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Provides interface for audit REST web services
 *
 * @author Yuriy Movchan Date: 05/24/2024
 */
public interface AuditRestWebService {

	@POST
	@Path("/audit/health")
	@Produces({ MediaType.APPLICATION_JSON })
	Response processHealthRequest(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context SecurityContext sec);

	@POST
	@Path("/audit//log")
	@Produces({ MediaType.APPLICATION_JSON })
	Response processLogRequest(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context SecurityContext sec);

	@POST
	@Path("/audit//telemetry")
	@Produces({ MediaType.APPLICATION_JSON })
	Response processTelemetryRequest(@Context HttpServletRequest request, @Context HttpServletResponse response,
			@Context SecurityContext sec);

}