/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;

import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.rest.model.AuthenticationMethod;
import io.jans.configapi.rest.resource.auth.BaseResource;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.slf4j.Logger;


@Path(ApiConstants.HEALTH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ApiHealthCheck extends BaseResource {
    
    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;
    
    @GET
    @Path(ApiConstants.LIVE)
    public Response getLivenessResponse() {
        log.debug("ApiHealthCheck::getLivenessResponse() - Entry");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", "jans-config-api liveness");
        jsonObject.put("status", "UP");
        log.debug("\n\n\n ApiHealthCheck::getLivenessResponse() - jsonObject = " + jsonObject + "\n\n");
        return Response.ok(jsonObject.toString()).build();
    }
    
    @GET
    @Path(ApiConstants.READY)
    public Response getReadinessResponse() {
        log.debug("ApiHealthCheck::getReadinessResponse() - Entry");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", "jans-config-api readiness");
        try {
            checkDatabaseConnection();
            
            jsonObject.put("status", "UP");
            log.debug("\n\n\n ApiHealthCheck::getReadinessResponse() - Success Response = " + jsonObject + "\n\n");
            return Response.ok(jsonObject.toString()).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            jsonObject.put("error", "e.getMessage()");
            log.debug("\n\n\n ApiHealthCheck::getReadinessResponse() - Error Response = " + jsonObject + "\n\n");
            return Response.ok(jsonObject.toString()).build();
        }        
    }

    private void checkDatabaseConnection() {
        configurationService.findConf();
    }
}
