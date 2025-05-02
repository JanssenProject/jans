package io.jans.lock.service.ws.rs.stat;

import io.jans.lock.model.core.LockApiError;
import io.jans.lock.model.stat.FlatStatResponse;
import io.jans.lock.util.ApiAccessConstants;
import io.jans.service.security.api.ProtectedApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Provides server with basic statistic
 *
 * @author Yuriy Movchan Date: 12/02/2024
 */
@Dependent
@Path("/internal/stat")
public interface StatRestWebService {

	@Operation(summary = "Request stat data", description = "Request stat data", tags = {
			"Lock - Stat" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_STAT_READ_ACCESS }))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FlatStatResponse.class, description = "StatFound"))),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@GET
	@ProtectedApi(scopes = { ApiAccessConstants.LOCK_STAT_READ_ACCESS })
	@Produces(MediaType.APPLICATION_JSON)
	public Response statGet(@QueryParam("month") String months, @QueryParam("start-month") String startMonth,
			@QueryParam("end-month") String endMonth, @QueryParam("format") String format);

	@Operation(summary = "Request stat data", description = "Request stat data", tags = {
			"Lock - Stat" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_STAT_READ_ACCESS }))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FlatStatResponse.class, description = "StatFound"))),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })
	@POST
	@ProtectedApi(scopes = { ApiAccessConstants.LOCK_STAT_READ_ACCESS })
	@Produces(MediaType.APPLICATION_JSON)
	public Response statPost(@FormParam("month") String months, @FormParam("start-month") String startMonth,
			@FormParam("end-month") String endMonth, @FormParam("format") String format);
}
