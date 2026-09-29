/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.ws.rs.trace;

import io.jans.core.cedarling.service.security.api.ProtectedCedarlingApi;
import io.jans.lock.model.core.LockApiError;
import io.jans.lock.model.trace.api.ProducerChainListResponse;
import io.jans.lock.model.trace.api.ProducerChainRegistrationRequest;
import io.jans.lock.model.trace.api.ProducerChainResponse;
import io.jans.lock.model.trace.api.ProducerKeyListResponse;
import io.jans.lock.model.trace.api.ProducerKeyRegistrationRequest;
import io.jans.lock.model.trace.api.ProducerKeyResponse;
import io.jans.lock.model.trace.api.ProducerKeyRevokeRequest;
import io.jans.lock.util.ApiAccessConstants;
import io.jans.service.security.api.ProtectedApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Admin REST API for TRACE producer-key and producer-chain registration/listing/revocation
 * (design §6, §8, TRACE MVP task 15), scoped to the admin client's evidence domain (design
 * decision D-1). Every method requires the {@code trace.admin} scope.
 *
 * <p>{@code producer_id} may contain {@code /}, so it is never carried in a path segment; every
 * identifier travels in the request body or as a query parameter.
 *
 * @author Yuriy Movchan
 */
@Path("/audit/trace/admin")
public interface TraceAdminRestWebService {

	@Operation(summary = "Register a producer key", description = "Register a producer's Ed25519 verification key", tags = {
			"Lock - Audit Trace Admin" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS }))
	@RequestBody(description = "Producer key registration request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProducerKeyRegistrationRequest.class)))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProducerKeyResponse.class))),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "403", description = "Forbidden"),
			@ApiResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "ConflictException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@POST
	@Path("/producer-keys")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = { ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS })
	@ProtectedCedarlingApi(action = "Jans::Action::\"POST\"", resource = "Jans::HTTP_Request", id = "lock_audit_trace_admin_key_write", path = "/audit/trace/admin/producer-keys")
	Response registerProducerKey(ProducerKeyRegistrationRequest request);

	@Operation(summary = "List producer keys", description = "List registered producer keys in the caller's evidence domain", tags = {
			"Lock - Audit Trace Admin" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS }))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProducerKeyListResponse.class))),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "403", description = "Forbidden"),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@GET
	@Path("/producer-keys")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = { ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS })
	@ProtectedCedarlingApi(action = "Jans::Action::\"GET\"", resource = "Jans::HTTP_Request", id = "lock_audit_trace_admin_key_read", path = "/audit/trace/admin/producer-keys")
	Response listProducerKeys(
			@Parameter(description = "Restrict the result to one producer id") @QueryParam("producer_id") String producerId);

	@Operation(summary = "Revoke a producer key", description = "Revoke a registered producer key (idempotent)", tags = {
			"Lock - Audit Trace Admin" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS }))
	@RequestBody(description = "Producer key revocation request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProducerKeyRevokeRequest.class)))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProducerKeyResponse.class))),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "403", description = "Forbidden"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@POST
	@Path("/producer-keys/revoke")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = { ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS })
	@ProtectedCedarlingApi(action = "Jans::Action::\"POST\"", resource = "Jans::HTTP_Request", id = "lock_audit_trace_admin_key_revoke", path = "/audit/trace/admin/producer-keys/revoke")
	Response revokeProducerKey(ProducerKeyRevokeRequest request);

	@Operation(summary = "Register a producer chain", description = "Pre-register a producer chain", tags = {
			"Lock - Audit Trace Admin" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS }))
	@RequestBody(description = "Producer chain registration request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProducerChainRegistrationRequest.class)))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProducerChainResponse.class))),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "403", description = "Forbidden"),
			@ApiResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "ConflictException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@POST
	@Path("/producer-chains")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = { ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS })
	@ProtectedCedarlingApi(action = "Jans::Action::\"POST\"", resource = "Jans::HTTP_Request", id = "lock_audit_trace_admin_chain_write", path = "/audit/trace/admin/producer-chains")
	Response registerProducerChain(ProducerChainRegistrationRequest request);

	@Operation(summary = "List producer chains", description = "List registered producer chains in the caller's evidence domain", tags = {
			"Lock - Audit Trace Admin" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS }))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProducerChainListResponse.class))),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "403", description = "Forbidden"),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@GET
	@Path("/producer-chains")
	@Produces({ MediaType.APPLICATION_JSON })
	@ProtectedApi(scopes = { ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS })
	@ProtectedCedarlingApi(action = "Jans::Action::\"GET\"", resource = "Jans::HTTP_Request", id = "lock_audit_trace_admin_chain_read", path = "/audit/trace/admin/producer-chains")
	Response listProducerChains(
			@Parameter(description = "Restrict the result to one producer id") @QueryParam("producer_id") String producerId);

}
