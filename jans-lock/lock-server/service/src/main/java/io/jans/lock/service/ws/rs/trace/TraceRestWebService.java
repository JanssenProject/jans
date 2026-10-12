/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.ws.rs.trace;

import java.io.InputStream;

import io.jans.core.cedarling.service.security.api.ProtectedCedarlingApi;
import io.jans.lock.model.trace.api.TraceAcceptanceResponse;
import io.jans.lock.model.trace.api.TraceBulkIngestionResponse;
import io.jans.lock.model.trace.api.TraceErrorResponse;
import io.jans.lock.model.trace.api.TraceExecutionResponse;
import io.jans.lock.model.trace.api.TraceRecordResponse;
import io.jans.lock.util.ApiAccessConstants;
import io.jans.service.security.api.ProtectedApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * REST API for TRACE record ingestion and retrieval (design §11.1-§11.3, TRACE MVP tasks 20, 21,
 * and bulk ingestion decision D-16). Write endpoints require the {@code trace.write} scope, read
 * endpoints the {@code trace.readonly} scope; the evidence domain is always derived from the
 * authenticated client's binding (design decision D-1), never from the request body.
 *
 * <p>Not exposed over gRPC (design decision D-15): no {@code grpcMethodName} is set.
 *
 * @author Yuriy Movchan
 */
@Path("/audit/trace")
public interface TraceRestWebService {

	@Operation(summary = "Submit a TRACE evidence record", description = "Verify and ingest a single signed TRACE assertion (design §7)", tags = {
			"Lock - Audit Trace" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_TRACE_WRITE_ACCESS }))
	@RequestBody(description = "TRACE assertion (design §7)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object")))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "202", description = "Accepted", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceAcceptanceResponse.class))),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))), })
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ProtectedApi(scopes = { ApiAccessConstants.LOCK_TRACE_WRITE_ACCESS })
	@ProtectedCedarlingApi(action = "Jans::Action::\"POST\"", resource = "Jans::HTTP_Request", id = "lock_audit_trace_write", path = "/audit/trace")
	Response submitRecord(InputStream body, @Context HttpServletRequest request, @Context SecurityContext sec);

	@Operation(summary = "Submit a batch of TRACE evidence records", description = "Verify and ingest a JSON array of signed TRACE assertions, continuing past per-item failures (design decision D-16)", tags = {
			"Lock - Audit Trace" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_TRACE_WRITE_ACCESS }))
	@RequestBody(description = "JSON array of TRACE assertions (design §7 shape, each)", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(type = "object"))))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "202", description = "Accepted (per-item outcomes in the body)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceBulkIngestionResponse.class))),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))), })
	@POST
	@Path("/bulk")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ProtectedApi(scopes = { ApiAccessConstants.LOCK_TRACE_WRITE_ACCESS })
	@ProtectedCedarlingApi(action = "Jans::Action::\"POST\"", resource = "Jans::HTTP_Request", id = "lock_audit_trace_bulk_write", path = "/audit/trace/bulk")
	Response submitBulkRecords(InputStream body, @Context HttpServletRequest request, @Context SecurityContext sec);

	@Operation(summary = "Retrieve one TRACE record", description = "Domain-scoped retrieval of a stored envelope by record identity (design §11.2)", tags = {
			"Lock - Audit Trace" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_TRACE_READ_ACCESS }))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceRecordResponse.class))),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))), })
	@GET
	@Path("/records/{record_id}")
	@Produces(MediaType.APPLICATION_JSON)
	@ProtectedApi(scopes = { ApiAccessConstants.LOCK_TRACE_READ_ACCESS })
	@ProtectedCedarlingApi(action = "Jans::Action::\"GET\"", resource = "Jans::HTTP_Request", id = "lock_audit_trace_record_read", path = "/audit/trace/records")
	Response getRecord(@PathParam("record_id") String recordId,
			@Parameter(description = "Disambiguate a bare record_id used by more than one producer") @QueryParam("producer_id") String producerId);

	@Operation(summary = "Retrieve one TRACE execution", description = "All direct records of an execution, ordered by receipt sequence (design §11.3)", tags = {
			"Lock - Audit Trace" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_TRACE_READ_ACCESS }))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceExecutionResponse.class))),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))),
			@ApiResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TraceErrorResponse.class))), })
	@GET
	@Path("/executions/{trace_execution_id}")
	@Produces(MediaType.APPLICATION_JSON)
	@ProtectedApi(scopes = { ApiAccessConstants.LOCK_TRACE_READ_ACCESS })
	@ProtectedCedarlingApi(action = "Jans::Action::\"GET\"", resource = "Jans::HTTP_Request", id = "lock_audit_trace_execution_read", path = "/audit/trace/executions")
	Response getExecution(@PathParam("trace_execution_id") String traceExecutionId,
			@Parameter(description = "Disambiguate a bare trace_execution_id used by more than one authority") @QueryParam("execution_authority") String executionAuthority,
			@Parameter(description = "Paging offset") @QueryParam("start") @DefaultValue("0") int start,
			@Parameter(description = "Paging size, 1-1000") @QueryParam("count") @DefaultValue("100") int count);

}
