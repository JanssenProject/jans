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

import io.jans.lock.model.audit.HealthEntry;
import io.jans.lock.model.audit.LogEntry;
import io.jans.lock.model.audit.TelemetryEntry;
import io.jans.lock.model.core.LockApiError;
import io.jans.lock.util.ApiAccessConstants;
import io.jans.lock.util.Constants;
import io.jans.service.security.api.ProtectedApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Path(Constants.BASE_PATH + "/audit")
public interface AuditRestWebService {

	@Operation(summary = "Save health data", description = "Save health data", tags = {
			"Lock - Audit Health" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_HEALTH_WRITE_ACCESS }))
	@RequestBody(description = "Health entry", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = HealthEntry.class)))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@POST
	@Path("/health")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = { "https://jans.io/oauth/lock/health.write" })
	Response processHealthRequest(@Context HttpServletRequest request, @Context HttpServletResponse response,
			@Context SecurityContext sec);

	@Operation(summary = "Bulk save health data", description = "Bulk save health data", tags = {
			"Lock - Audit Health" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_HEALTH_WRITE_ACCESS }))
	@RequestBody(description = "Bulk health entry", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = HealthEntry.class))))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@POST
	@Path("/health/bulk")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = { "https://jans.io/oauth/lock/health.write" })
	Response processBulkHealthRequest(@Context HttpServletRequest request, @Context HttpServletResponse response,
			@Context SecurityContext sec);

	@Operation(summary = "Save log data", description = "Save log data", tags = {
			"Lock - Audit Log" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_LOG_WRITE_ACCESS }))
	@RequestBody(description = "Log entry", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LogEntry.class)))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@POST
	@Path("/log")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = { "https://jans.io/oauth/lock/log.write" })
	Response processLogRequest(@Context HttpServletRequest request, @Context HttpServletResponse response,
			@Context SecurityContext sec);

	@Operation(summary = "Bulk save log data", description = "Bulk save log data", tags = {
			"Lock - Audit Log" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_LOG_WRITE_ACCESS }))
	@RequestBody(description = "Bulk log entry", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = LogEntry.class))))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@POST
	@Path("/log/bulk")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = { "https://jans.io/oauth/lock/log.write" })
	Response processBulkLogRequest(@Context HttpServletRequest request, @Context HttpServletResponse response,
			@Context SecurityContext sec);

	@Operation(summary = "Save telemetry data", description = "Save telemetry data", tags = {
			"Lock - Audit Telemetry" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_TELEMETRY_WRITE_ACCESS }))
	@RequestBody(description = "Telemetry entry", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TelemetryEntry.class)))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@POST
	@Path("/telemetry")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = { "https://jans.io/oauth/lock/telemetry.write" })
	Response processTelemetryRequest(@Context HttpServletRequest request, @Context HttpServletResponse response,
			@Context SecurityContext sec);

	@Operation(summary = "Bulk save telemetry data", description = "Bulk save telemetry data", tags = {
			"Lock - Audit Telemetry" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_TELEMETRY_WRITE_ACCESS }))
	@RequestBody(description = "Bulk telemetry entry", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = TelemetryEntry.class))))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@POST
	@Path("/telemetry/bulk")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = { "https://jans.io/oauth/lock/telemetry.write" })
	Response processBulkTelemetryRequest(@Context HttpServletRequest request, @Context HttpServletResponse response,
			@Context SecurityContext sec);

}