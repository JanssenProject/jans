package io.jans.configapi.rest.resource.auth;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.auth.StatisticService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.slf4j.Logger;

@Path(ApiConstants.STATISTICS)
public class StatResource extends BaseResource {

    private final String statUrl = "/jans-auth/restv1/internal/stat";

    @Inject
    Logger logger;

    @Inject
    ConfigurationService configurationService;

    @Inject
    StatisticService statisticService;

    @GET
    @Path(ApiConstants.USER)
    @ProtectedApi(scopes = { ApiAccessConstants.STATS_USER_READ_ACCESS })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserStatistics(@HeaderParam("Authorization") String authorization, String month, String format) {
        logger.error("\n\n StatResource:::getUserStatistics() - authorization = " + authorization + " , month = "
                + month + " , format = " + format);
        String url = getIssuer() + this.statUrl;

        JsonNode jsonNode = this.statisticService.getUserStat(url, authorization, month, format);
        logger.debug("\n\n\n StatResource::getUserStatistics() - jsonNode = " + jsonNode + "\n\n");
        return Response.ok(jsonNode).build();
    }

    private String getIssuer() {
        return configurationService.find().getIssuer();
    }

}
