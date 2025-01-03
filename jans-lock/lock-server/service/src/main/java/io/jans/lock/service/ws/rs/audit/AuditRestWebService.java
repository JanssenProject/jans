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

package io.jans.lock.service.ws.rs.audit;

import io.jans.service.security.api.ProtectedApi;
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
@Path("/audit")
public interface AuditRestWebService {

	@POST
	@Path("/health")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = {"https://jans.io/oauth/lock/health.write"})
	Response processHealthRequest(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context SecurityContext sec);

	@POST
	@Path("/health/bulk")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = {"https://jans.io/oauth/lock/health.write"})
	Response processBulkHealthRequest(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context SecurityContext sec);

	@POST
	@Path("/log")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = {"https://jans.io/oauth/lock/log.write"})
	Response processLogRequest(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context SecurityContext sec);

	@POST
	@Path("/log/bulk")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = {"https://jans.io/oauth/lock/log.write"})
	Response processBulkLogRequest(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context SecurityContext sec);

	@POST
	@Path("/telemetry")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = {"https://jans.io/oauth/lock/telemetry.write"})
	Response processTelemetryRequest(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context SecurityContext sec);

	@POST
	@Path("/telemetry/bulk")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = {"https://jans.io/oauth/lock/telemetry.write"})
	Response processBulkTelemetryRequest(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context SecurityContext sec);

}