/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.lock.rest;


import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.lock.service.AuditService;
import io.jans.configapi.plugin.lock.util.Constants;
import io.jans.configapi.util.ApiConstants;
import io.jans.lock.model.audit.HealthEntry;
import io.jans.lock.model.audit.LogEntry;
import io.jans.lock.model.audit.TelemetryEntry;
import io.jans.orm.PersistenceEntryManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Path(Constants.AUDIT)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuditResource extends BaseResource {

    private static final String EVENT_START_DATE_ISO8601 = "eventStartDateIso8601";
    private static final String EVENT_END_DATE_ISO8601 = "eventEndDateIso8601";
    private static final String EVENT_START_DATE_PARSE_ERR = "Can't parse event start date !";
    private static final String EVENT_END_DATE_PARSE_ERR = "Can't parse event end date !";


	@Inject
	Logger logger;

	@Inject
	AuditService auditService;

	@Inject
	@Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
	PersistenceEntryManager persistenceEntryManager;

	@Operation(summary = "Save health data", description = "Save health data", operationId = "save-health-data", tags = {
			"Lock - Audit" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					Constants.LOCK_HEALTH_WRITE_ACCESS }))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
	@POST
	@ProtectedApi(scopes = { Constants.LOCK_HEALTH_WRITE_ACCESS })
	@Path(Constants.HEALTH)
	public Response postHealthData(@Valid HealthEntry healthEntry) {
		logger.debug("Save Health Data - healthEntry:{}", healthEntry);

		auditService.addHealthEntry(healthEntry);

		return Response.status(Response.Status.OK).build();
	}

	@Operation(summary = "Bulk save health data", description = "Bulk save health data", operationId = "bulk-save-health-data", tags = {
			"Lock - Audit" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					Constants.LOCK_HEALTH_WRITE_ACCESS }))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
	@POST
	@ProtectedApi(scopes = { Constants.LOCK_HEALTH_WRITE_ACCESS })
	@Path(Constants.HEALTH + Constants.BULK)
	public Response postBulkHealthData(@Valid List<HealthEntry> healthEntries) {
		logger.debug("Bulk save Health Data - healthEntries:{}", healthEntries);

		for (HealthEntry healthEntry: healthEntries) {
			auditService.addHealthEntry(healthEntry);
		}

		return Response.status(Response.Status.OK).build();
	}

	@Operation(summary = "Rerquest health records for specific event range", description = "Rerquest health records for specific event range", operationId = "request-lock-health-records-event-range", tags = {
			"Lock - Audit" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					Constants.LOCK_HEALTH_READ_ACCESS }))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = HealthEntry.class)))),
			@ApiResponse(responseCode = "400", description = "Wrong date range specified"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "InternalServerError") })
	@GET
	@ProtectedApi(scopes = { Constants.LOCK_HEALTH_READ_ACCESS })
	@Path(Constants.HEALTH + Constants.SEARCH)
	public Response getHealthEntrysByRange(
			@Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(ApiConstants.LIMIT) int limit,
			@Parameter(description = "Event start date in ISO8601 format") @QueryParam("eventStartDate") @NotNull String eventStartDateIso8601,
			@Parameter(description = "Event end date in ISO8601 format") @QueryParam("eventEndDate") @NotNull String eventEndDateIso8601) {
		logger.debug("Get Health entries by by event range");

        checkNotNull(eventStartDateIso8601, EVENT_START_DATE_ISO8601);
        checkNotNull(eventEndDateIso8601, EVENT_END_DATE_ISO8601);

		Date eventStartDate = decodeTime(eventStartDateIso8601);
		checkResourceNotNull(eventStartDate, EVENT_START_DATE_PARSE_ERR);

		Date eventEndDate = decodeTime(eventEndDateIso8601);
		checkResourceNotNull(eventStartDate, EVENT_END_DATE_PARSE_ERR);

		List<HealthEntry> entries = auditService.getHealthEntrysByRange(eventStartDate, eventEndDate, limit);

		return Response.ok(entries).build();
	}

	@Operation(summary = "Save log data", description = "Save log data", operationId = "save-log-data", tags = {
			"Lock - Audit" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					Constants.LOCK_LOG_WRITE_ACCESS }))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
	@POST
	@ProtectedApi(scopes = { Constants.LOCK_LOG_WRITE_ACCESS })
	@Path(Constants.LOG)
	public Response postLogData(@Valid LogEntry logEntry) {
		logger.debug("Save Log Data - logEntry:{}", logEntry);

		auditService.addLogData(logEntry);

		return Response.status(Response.Status.OK).build();
	}

	@Operation(summary = "Bulk save log data", description = "Bulk save log data", operationId = "bulk-save-log-data", tags = {
			"Lock - Audit" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					Constants.LOCK_LOG_WRITE_ACCESS }))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
	@POST
	@ProtectedApi(scopes = { Constants.LOCK_LOG_WRITE_ACCESS })
	@Path(Constants.LOG + Constants.BULK)
	public Response postBulkLogData(@Valid List<LogEntry> logEntries) {
		logger.debug("Bulk save Log Data - logEntries:{}", logEntries);

		for (LogEntry LogEntry : logEntries) {
			auditService.addLogData(LogEntry);
		}

		return Response.status(Response.Status.OK).build();
	}

	@Operation(summary = "Rerquest log records for specific event range", description = "Rerquest log records for specific event range", operationId = "request-lock-log-records-event-range", tags = {
			"Lock - Log" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					Constants.LOCK_LOG_READ_ACCESS }))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = LogEntry.class)))),
			@ApiResponse(responseCode = "400", description = "Wrong date range specified"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "InternalServerError") })
	@GET
	@ProtectedApi(scopes = { Constants.LOCK_LOG_READ_ACCESS })
	@Path(Constants.LOG + Constants.SEARCH)
	public Response getLogEntrysByRange(
			@Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(ApiConstants.LIMIT) int limit,
			@Parameter(description = "Event start date in ISO8601 format") @QueryParam("eventStartDate") @NotNull String eventStartDateIso8601,
			@Parameter(description = "Event end date in ISO8601 format") @QueryParam("eventEndDate") @NotNull String eventEndDateIso8601) {
		logger.debug("Get Log entries by by event range");

        checkNotNull(eventStartDateIso8601, EVENT_START_DATE_ISO8601);
        checkNotNull(eventEndDateIso8601, EVENT_END_DATE_ISO8601);

		Date eventStartDate = decodeTime(eventStartDateIso8601);
		checkResourceNotNull(eventStartDate, EVENT_START_DATE_PARSE_ERR);

		Date eventEndDate = decodeTime(eventEndDateIso8601);
		checkResourceNotNull(eventStartDate, EVENT_END_DATE_PARSE_ERR);

		List<LogEntry> entries = auditService.getLogEntrysByRange(eventStartDate, eventEndDate, limit);

		return Response.ok(entries).build();
	}

	@Operation(summary = "Save telemetry data", description = "Save telemetry data", operationId = "save-telemetry-data", tags = {
			"Lock - Audit" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					Constants.LOCK_TELEMETRY_WRITE_ACCESS }))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
	@POST
	@ProtectedApi(scopes = { Constants.LOCK_TELEMETRY_WRITE_ACCESS })
	@Path(Constants.TELEMETRY)
	public Response postTelemetryData(@Valid TelemetryEntry telemetryEntry) {
		logger.debug("Save Telemetry Data - telemetryEntry:{}", telemetryEntry);

		auditService.addTelemetryData(telemetryEntry);

		return Response.status(Response.Status.OK).build();
	}

	@Operation(summary = "Bulk save telemetry data", description = "Bulk save telemetry data", operationId = "bulk-save-telemetry-data", tags = {
			"Lock - Audit" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					Constants.LOCK_TELEMETRY_WRITE_ACCESS }))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
	@POST
	@ProtectedApi(scopes = { Constants.LOCK_TELEMETRY_WRITE_ACCESS })
	@Path(Constants.TELEMETRY + Constants.BULK)
	public Response postBulkTelemetryData(@Valid List<TelemetryEntry> telemetryEntries) {
		logger.debug("Bulk save Telemetry Data - telemetryEntries:{}", telemetryEntries);

		for (TelemetryEntry telemetryEntry : telemetryEntries) {
			auditService.addTelemetryData(telemetryEntry);
		}

		return Response.status(Response.Status.OK).build();
	}

	@Operation(summary = "Request telemetry records for specific event range", description = "Rerquest telemetry records for specific event range", operationId = "request-lock-telemetry-records-event-range", tags = {
			"Lock - Audit" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					Constants.LOCK_TELEMETRY_READ_ACCESS }))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = TelemetryEntry.class)))),
			@ApiResponse(responseCode = "400", description = "Wrong date range specified"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "500", description = "InternalServerError") })
	@GET
	@ProtectedApi(scopes = { Constants.LOCK_TELEMETRY_READ_ACCESS })
	@Path(Constants.TELEMETRY + Constants.SEARCH)
	public Response getTelemetryEntrysByRange(
			@Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(ApiConstants.LIMIT) int limit,
			@Parameter(description = "Event start date in ISO8601 format") @QueryParam("eventStartDate") @NotNull String eventStartDateIso8601,
			@Parameter(description = "Event end date in ISO8601 format") @QueryParam("eventEndDate") @NotNull String eventEndDateIso8601) {
		logger.debug("Get Telemetry entries by by event range");

        checkNotNull(eventStartDateIso8601, EVENT_START_DATE_ISO8601);
        checkNotNull(eventEndDateIso8601, EVENT_END_DATE_ISO8601);

		Date eventStartDate = decodeTime(eventStartDateIso8601);
		checkResourceNotNull(eventStartDate, EVENT_START_DATE_PARSE_ERR);

		Date eventEndDate = decodeTime(eventEndDateIso8601);
		checkResourceNotNull(eventStartDate, EVENT_END_DATE_PARSE_ERR);

		List<TelemetryEntry> entries = auditService.getTelemetryEntrysByRange(eventStartDate, eventEndDate, limit);

		return Response.ok(entries).build();
	}

	private Date decodeTime(String dateZ) {
		try {
			return new Date(Instant.parse(dateZ).toEpochMilli());
		} catch (DateTimeParseException ex) {
			logger.error("Failed to decode ISO-8601 date '{}'", dateZ, ex);

			return null;
		}
	}


}