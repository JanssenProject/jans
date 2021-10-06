package io.jans.configapi.rest.resource.auth;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.auth.AuthService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@Path(ApiConstants.JANS_AUTH + ApiConstants.HEALTH)
public class HealthCheckResource extends BaseResource {

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
        Response response = authService.getHealthCheckResponse(url);
        logger.error("StatResource::getUserStatistics() - response:{} ",response);
        return Response.ok(response).build();
    }

    private String getIssuer() {
        return configurationService.find().getIssuer();
    }

}
