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

package io.jans.lock.service.ws.rs;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.model.core.LockApiError;
import io.jans.lock.service.config.ConfigurationService;
import io.jans.lock.service.ws.rs.base.BaseResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

/**
 * Lock metadata configuration
 *
 * @author Yuriy Movchan Date: 12/19/2018
 */
@Dependent
@Path("/configuration")
public class ConfigurationRestWebService extends BaseResource {

	@Inject
	private ConfigurationService configurationService;

	@Operation(summary = "Request .well-known data", description = "Request .well-know Lock server configuration", tags = {
			"Lock - Server Configuration" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class, description = "ConfigurationFound"))),
			@ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LockApiError.class, description = "InternalServerError"))), })

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getConfiguration() {
		ObjectNode response = configurationService.getLockConfiguration();

		ResponseBuilder builder = Response.ok().entity(response.toString());
		return builder.build();
	}

}
