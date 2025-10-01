/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.lock.service.ws.rs.config;

import org.slf4j.Logger;

import io.jans.lock.util.ServerUtil;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Provides interface for сщташп REST web services
 *
 * @author Yuriy Movchan Date: 06/06/2024
 */
@Dependent
public class ConfigRestWebServiceImpl implements ConfigRestWebService {

	@Inject
	private Logger log;

	@Override
	public Response processConfigRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
		log.debug("Processing Config request");
		Response.ResponseBuilder builder = Response.ok();

		builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
		builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
		builder.entity("{\"res\" : \"ok_config\"}");

		return builder.build();
	}

	@Override
	public Response processIssuersRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
		log.debug("Processing Issuers request");
		Response.ResponseBuilder builder = Response.ok();

		builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
		builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
		builder.entity("{\"res\" : \"ok_issuers\"}");

		return builder.build();
	}

	@Override
	public Response processSchemaRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
		log.debug("Processing Schema request");
		Response.ResponseBuilder builder = Response.ok();

		builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
		builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
		builder.entity("{\"res\" : \"ok_schema\"}");

		return builder.build();
	}

	@Override
	public Response processPolicyRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
		log.debug("Processing Policy request");
		Response.ResponseBuilder builder = Response.ok();

		builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
		builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
		builder.entity("{\"res\" : \"ok_policy\"}");

		return builder.build();
	}


}
