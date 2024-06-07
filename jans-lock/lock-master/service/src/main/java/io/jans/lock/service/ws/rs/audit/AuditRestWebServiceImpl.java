/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.lock.service.ws.rs.audit;

import org.slf4j.Logger;

import io.jans.lock.service.util.ServerUtil;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Provides interface for audit REST web services
 *
 * @author Yuriy Movchan Date: 06/06/2024
 */
public class AuditRestWebServiceImpl implements AuditRestWebService {

	@Inject
	private Logger log;

	@Override
	public Response processHealthRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
		log.debug("Processing Health request");
		Response.ResponseBuilder builder = Response.ok();

		builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
		builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
		builder.entity("{\"res\" : \"ok\"}");

		return builder.build();
	}

	@Override
	public Response processLogRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
		log.debug("Processing Log request");
		Response.ResponseBuilder builder = Response.ok();

		builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
		builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
		builder.entity("{\"res\" : \"ok\"}");

		return builder.build();
	}

	@Override
	public Response processTelemetryRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
		log.debug("Processing Telemetry request");
		Response.ResponseBuilder builder = Response.ok();

		builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
		builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
		builder.entity("{\"res\" : \"ok\"}");

		return builder.build();
	}

}
