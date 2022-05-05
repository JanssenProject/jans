package io.jans.configapi.rest.resource.auth;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.auth.AuthService;
import io.jans.configapi.util.ApiConstants;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

@Path(ApiConstants.JANS_AUTH + ApiConstants.HEALTH)
public class HealthCheckResource extends ConfigBaseResource {

    private static final String HEALTH_CHECK_URL = "/jans-auth/sys/health-check";

    @Inject
    Logger logger;

    @Inject
    ConfigurationService configurationService;

    @Inject
    AuthService authService;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHealthCheckStatus() {      
        String url = getIssuer() + HEALTH_CHECK_URL;
        JsonNode jsonNode = authService.getHealthCheckResponse(url);
        logger.debug("StatResource::getUserStatistics() - jsonNode:{} ",jsonNode);
        return Response.ok(jsonNode).build();
    }

    private String getIssuer() {
        return configurationService.find().getIssuer();
    }

}
