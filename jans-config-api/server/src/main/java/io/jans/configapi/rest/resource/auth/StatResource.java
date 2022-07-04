package io.jans.configapi.rest.resource.auth;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.auth.AuthService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@Path(ApiConstants.STATISTICS)
public class StatResource extends ConfigBaseResource {

    private final String statUrl = "/jans-auth/restv1/internal/stat";

    @Inject
    Logger logger;

    @Inject
    ConfigurationService configurationService;

    @Inject
    AuthService authService;

    @GET
    @ProtectedApi(scopes = {ApiAccessConstants.STATS_USER_READ_ACCESS, ApiAccessConstants.JANS_STAT})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatistics(@HeaderParam("Authorization") String authorization,
                                  @QueryParam(value = "month") String month,
                                  @QueryParam(value = "start_month") String startMonth,
                                  @QueryParam(value = "end_month") String endMonth,
                                  @QueryParam(value = "format") String format) {
        if (StringUtils.isBlank(format)) {
            format = "";
        }
        String url = getIssuer() + this.statUrl;
        JsonNode jsonNode = this.authService.getStat(url, authorization, month, startMonth, endMonth, format);
        logger.trace("StatResource::getUserStatistics() - jsonNode:{} ", jsonNode);
        return Response.ok(jsonNode.get("response")).build();
    }

    private String getIssuer() {
        return configurationService.find().getIssuer();
    }

}
