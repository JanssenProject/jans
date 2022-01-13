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

@Path(ApiConstants.STATISTICS)
public class StatResource extends BaseResource {

    private final String statUrl = "/jans-auth/restv1/internal/stat";

    @Inject
    Logger logger;

    @Inject
    ConfigurationService configurationService;

    @Inject
    AuthService authService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.STATS_USER_READ_ACCESS, ApiAccessConstants.JANS_STAT })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatistics(@HeaderParam("Authorization") String authorization,
            @QueryParam(value = "month") String month, @QueryParam(value = "format") String format) {
        if (StringUtils.isBlank(format)) {
            format = "";
        }
        String url = getIssuer() + this.statUrl;
        JsonNode jsonNode = this.authService.getStat(url, authorization, month, format);
        logger.trace("StatResource::getUserStatistics() - jsonNode:{} ",jsonNode);
        return Response.ok(jsonNode.get("response")).build();
    }

    private String getIssuer() {
        return configurationService.find().getIssuer();
    }

}
