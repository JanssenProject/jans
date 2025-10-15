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

package io.jans.lock.ws.rs;

import org.slf4j.Logger;

import io.jans.model.status.StatsData;
import io.jans.orm.PersistenceEntryManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Health check controller
 * 
 * @author Yuriy Movchan Date: 12/12/2023
 */
@ApplicationScoped
@Path("/")
public class HealthCheckController {

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

    @Inject
    Logger logger;

    @Inject
    @RequestScoped
    private StatsData statsData;

    @Operation(summary = "Returns application server status", description = "Returns application server status", operationId = "get-server-stat", tags = {
    "Health - Check" })
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = StatsData.class))),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path("/server-stat")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getServerStat() {
        logger.debug("Server Stat - statsData:{}", statsData);

        StatsData clonedStatsData = new StatsData();
        clonedStatsData.setDbType(statsData.getDbType());
        clonedStatsData.setFacterData(statsData.getFacterData());
        clonedStatsData.setLastUpdate(statsData.getLastUpdate());

        return Response.ok(clonedStatsData).build();

    }

    @GET
    @POST
    @Path("/health-check")
    @Produces(MediaType.APPLICATION_JSON)
	public String healthCheckController() {
    	boolean isConnected = persistenceEntryManager.getOperationService().isConnected();
    	String dbStatus = isConnected ? "online" : "offline"; 
        return "{\"status\": \"running\", \"db_status\":\"" + dbStatus + "\"}";
	}

}
