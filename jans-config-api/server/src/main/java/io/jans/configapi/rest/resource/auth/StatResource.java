package io.jans.configapi.rest.resource.auth;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.auth.AuthService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AuthUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static io.jans.as.model.util.Util.escapeLog;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;


@Path(ApiConstants.STATISTICS)
public class StatResource extends ConfigBaseResource {

    private static final String STAT_URL = "/jans-auth/restv1/internal/stat";

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    @Inject
    AuthService authService;

    /**
     * Fetches basic server statistics for a specified month or month range.
     *
     * @param authorization the Authorization header value used to authenticate the request
     * @param month         month for which the stat report is requested; required if both start_month and end_month are absent (format: YYYYMM)
     * @param startMonth    start month of the range for which the stat report is requested (format: YYYYMM)
     * @param endMonth      end month of the range for which the stat report is requested (format: YYYYMM)
     * @param format        report format; an empty value requests the default format
     * @return              the JSON value of the "response" field containing the requested statistics
     */
    @Operation(summary = "Provides server with basic statistic", description = "Provides server with basic statistic", operationId = "get-stat", tags = {
            "Statistics - User" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.STATS_USER_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JANS_STAT }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.STATS_USER_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stats", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonNode.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.STATS_USER_READ_ACCESS,
            ApiAccessConstants.JANS_STAT }, groupScopes = {}, superScopes = {
                    ApiAccessConstants.STATS_USER_ADMIN_ACCESS, ApiAccessConstants.SUPER_ADMIN_READ_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatistics(@Parameter(description = "Authorization code") @HeaderParam("Authorization") String authorization,
            @Parameter(description = "Month for which the stat report is to be fetched. The parameter is mandatory if start_month and end_month parameters are not present.", example="202012 (2020 Dec) 202101 (2021 Jan)") @QueryParam(value = "month") String month,
            @Parameter(description = "Start-Month for which the stat report is to be fetched") @QueryParam(value = "start_month") String startMonth,
            @Parameter(description = "End-Month for which the stat report is to be fetched") @QueryParam(value = "end_month") String endMonth,
            @Parameter(description = "Report format") @QueryParam(value = "format") String format) {
        if (log.isDebugEnabled()) {
            log.debug("Statistics search param - month:{}, startMonth:{}, endMonth:{}, format:{}", escapeLog(month),
                    escapeLog(startMonth), escapeLog(endMonth), escapeLog(format));
        }

        if (StringUtils.isBlank(format)) {
            format = "";
        }

        if (StringUtils.isBlank(month) || month.equalsIgnoreCase("null")) {
            month = "";
        }

        if (StringUtils.isBlank(startMonth) || startMonth.equalsIgnoreCase("null")) {
            startMonth = "";
        }
        
        if (StringUtils.isBlank(endMonth) || endMonth.equalsIgnoreCase("null")) {
            endMonth = "";
        }
        
        JsonNode jsonNode = null;
        StringBuilder sb = new StringBuilder().append(" month:{").append(month).append("}").append(", startMonth:{")
                .append(startMonth).append("}").append(", endMonth:{").append(endMonth).append("}").append(", format:{")
                .append(format).append("}");
        try {
            String url = getIssuer() + STAT_URL;
            jsonNode = this.authService.getStat(url, authorization, month, startMonth, endMonth, format);
            log.debug("StatResource::getUserStatistics() - jsonNode:{} ", jsonNode);
            return Response.ok(jsonNode.get("response")).build();
        } catch (WebApplicationException wex) {
            sb.append(" ApplicationException while fetching stats is - ").append("wex.getResponse().getStatus():{}")
                    .append(wex.getResponse().getStatus()).append(wex.getResponse().getEntity()).append(", is:{")
                    .append(AuthUtil.getStackTraceAsString(wex)).append("}");

            log.error(sb.toString());
            throw new WebApplicationException(sb.toString(), wex.getResponse().getStatus());
        } catch (Exception ex) {
            ex.printStackTrace();
            sb.append(" Exception while fetching stats is - ").append(", is:{")
            .append(AuthUtil.getStackTraceAsString(ex)).append("}");

            log.error(sb.toString());
            throwInternalServerException(ex);
        }
        return Response.ok(jsonNode).build();
    }

    private String getIssuer() {
        return configurationService.find().getIssuer();
    }

}