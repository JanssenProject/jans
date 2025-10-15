package io.jans.lock.service.ws.rs.policy;

import io.jans.lock.cedarling.service.security.api.ProtectedCedarlingApi;
import io.jans.lock.model.core.LockApiError;
import io.jans.lock.util.ApiAccessConstants;
import io.jans.service.security.api.ProtectedApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.enterprise.context.Dependent;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Provides server with basic statistic
 *
 * @author Yuriy Movchan Date: 12/02/2024
 */
@Dependent
@Path("/policy")
public interface PolicyRestWebService {

	@Operation(summary = "Request policies URI list", description = "Request policies URI list", tags = {
			"Lock - Policy" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_POLICY_READ_ACCESS }))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class, description = "PolicyFound"))),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@GET
	@ProtectedApi(scopes = { ApiAccessConstants.LOCK_POLICY_READ_ACCESS })
	@ProtectedCedarlingApi(action = "Jans::Action::\"GET\"", resource = "Jans::HTTP_Request", id="lock_policy_list", path="/policy")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPoliciesUriList();

	@Operation(summary = "Request policy data", description = "Request policy data", tags = {
			"Lock - Policy" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_POLICY_READ_ACCESS }))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class, description = "PolicyFound"))),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@GET
	@ProtectedApi(scopes = { ApiAccessConstants.LOCK_POLICY_READ_ACCESS })
	@ProtectedCedarlingApi(action = "Jans::Action::\"GET\"", resource = "Jans::HTTP_Request", id="lock_policy_get_by_id", path="/policy")
	@Produces(MediaType.APPLICATION_JSON)
    @Path(ApiAccessConstants.URI_PATH)
	public Response getPolicyByUri(@Parameter(description = "Policy URI") @PathParam(ApiAccessConstants.URI) @NotNull String uri);

}
