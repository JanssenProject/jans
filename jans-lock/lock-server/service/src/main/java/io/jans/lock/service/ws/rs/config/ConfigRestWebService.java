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

import io.jans.service.security.api.ProtectedApi;
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
	@ProtectedApi(scopes = {"https://jans.io/lock-server/config.read"})
	Response processConfigRequest(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context SecurityContext sec);

	@GET
	@Path("/config/issuers")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = {"https://jans.io/lock-server/issuers.read"})
	Response processIssuersRequest(@Context HttpServletRequest request, @Context HttpServletResponse response,
			@Context SecurityContext sec);

	@GET
	@Path("/config/schema")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = {"https://jans.io/lock-server/schema.read"})
	Response processSchemaRequest(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context SecurityContext sec);

	@GET
	@Path("/config/policy")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = {"https://jans.io/lock-server/policy.read"})
	Response processPolicyRequest(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context SecurityContext sec);

}