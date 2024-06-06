/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.lock.service.ws.rs.config;

import org.slf4j.Logger;

import io.jans.lock.service.util.ServerUtil;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Provides interface for сщташп REST web services
 *
 * @author Yuriy Movchan Date: 06/06/2024
 */
public class ConfigRestWebServiceImpl implements ConfigRestWebService {

	@Inject
	private Logger log;

	@Override
	public Response processConfigRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
		log.debug("Processing Config request");
		Response.ResponseBuilder builder = Response.ok();

		builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
		builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
		builder.entity("{\"res\" : \"ok\"}");

		return builder.build();
	}

	@Override
	public Response processIssuersRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
		log.debug("Processing Issuers request");
		Response.ResponseBuilder builder = Response.ok();

		builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
		builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
		builder.entity("{\"res\" : \"ok\"}");

		return builder.build();
	}

	@Override
	public Response processPolicyRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
		log.debug("Processing Policy request");
		Response.ResponseBuilder builder = Response.ok();

		builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
		builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
		builder.entity("{\"res\" : \"ok\"}");

		return builder.build();
	}

	@Override
	public Response processSchemaRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
		log.debug("Processing Schema request");
		Response.ResponseBuilder builder = Response.ok();

		builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
		builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
		builder.entity("{\"res\" : \"ok\"}");

		return builder.build();
	}

}
