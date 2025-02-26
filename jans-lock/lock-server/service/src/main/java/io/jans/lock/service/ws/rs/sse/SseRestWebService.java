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

package io.jans.lock.service.ws.rs.sse;

import io.jans.lock.model.core.LockApiError;
import io.jans.lock.util.ApiAccessConstants;
import io.jans.service.security.api.ProtectedApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

/**
 * @author Yuriy Movchan Date: 05/24/2024
 */
public interface SseRestWebService {

	@Operation(summary = "Subscribe to SSE events", description = "Subscribe to SSE events", tags = {
			"Lock - SSE" }, security = @SecurityRequirement(name = "oauth2", scopes = {
					ApiAccessConstants.LOCK_STAT_READ_ACCESS }))
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "BadRequestException"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "NotFoundException"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })

	@GET
	@Path("/sse")
	@Produces(MediaType.SERVER_SENT_EVENTS)
	@ProtectedApi(scopes = { ApiAccessConstants.LOCK_SSE_READ_ACCESS })
	public void subscribe(@Context Sse sse, @Context SseEventSink sseEventSink);

}