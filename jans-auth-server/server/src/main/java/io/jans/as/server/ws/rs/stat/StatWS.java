package io.jans.as.server.ws.rs.stat;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.server.model.common.AbstractToken;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.service.stat.StatResponseService;
import io.jans.as.server.service.stat.StatService;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.util.ServerUtil;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.common.TextFormat;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static io.jans.as.model.util.Util.escapeLog;

/**
 * Provides server with basic statistic.
 * <p>
 * https://github.com/GluuFederation/oxAuth/issues/1512
 * https://github.com/GluuFederation/oxAuth/issues/1321
 *
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@Path("/internal/stat")
public class StatWS {

    @Inject
    private Logger log;

    @Inject
    private StatResponseService statResponseService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private TokenService tokenService;

    public static String createOpenMetricsResponse(StatResponse statResponse) throws IOException {
        Writer writer = new StringWriter();
        CollectorRegistry registry = new CollectorRegistry();

        final Counter usersCounter = Counter.build()
                .name("monthly_active_users")
                .labelNames(Constants.MONTH)
                .help("Monthly active users")
                .register(registry);

        final Counter accessTokenCounter = Counter.build()
                .name(StatService.ACCESS_TOKEN_KEY)
                .labelNames(Constants.MONTH, Constants.GRANTTYPE)
                .help("Access Token")
                .register(registry);

        final Counter idTokenCounter = Counter.build()
                .name(StatService.ID_TOKEN_KEY)
                .labelNames(Constants.MONTH, Constants.GRANTTYPE)
                .help("Id Token")
                .register(registry);

        final Counter refreshTokenCounter = Counter.build()
                .name(StatService.REFRESH_TOKEN_KEY)
                .labelNames(Constants.MONTH, Constants.GRANTTYPE)
                .help("Refresh Token")
                .register(registry);

        final Counter umaTokenCounter = Counter.build()
                .name(StatService.UMA_TOKEN_KEY)
                .labelNames(Constants.MONTH, Constants.GRANTTYPE)
                .help("UMA Token")
                .register(registry);

        for (Map.Entry<String, StatResponseItem> entry : statResponse.getResponse().entrySet()) {
            final String month = entry.getKey();
            final StatResponseItem item = entry.getValue();

            usersCounter
                    .labels(month)
                    .inc(item.getMonthlyActiveUsers());

            for (Map.Entry<String, Map<String, Long>> tokenEntry : item.getTokenCountPerGrantType().entrySet()) {
                final String grantType = tokenEntry.getKey();
                final Map<String, Long> tokenMap = tokenEntry.getValue();

                accessTokenCounter
                        .labels(month, grantType)
                        .inc(getToken(tokenMap, StatService.ACCESS_TOKEN_KEY));

                idTokenCounter
                        .labels(month, grantType)
                        .inc(getToken(tokenMap, StatService.ID_TOKEN_KEY));

                refreshTokenCounter
                        .labels(month, grantType)
                        .inc(getToken(tokenMap, StatService.REFRESH_TOKEN_KEY));

                umaTokenCounter
                        .labels(month, grantType)
                        .inc(getToken(tokenMap, StatService.UMA_TOKEN_KEY));
            }
        }

        TextFormat.write004(writer, registry.metricFamilySamples());
        return writer.toString();
    }

    private static long getToken(Map<String, Long> map, String key) {
        Long v = map.get(key);
        return v != null ? v : 0;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response statGet(@HeaderParam("Authorization") String authorization,
                            @QueryParam("month") String months,
                            @QueryParam("start-month") String startMonth,
                            @QueryParam("end-month") String endMonth,
                            @QueryParam("format") String format) {
        return stat(authorization, months, startMonth, endMonth, format);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response statPost(@HeaderParam("Authorization") String authorization,
                             @FormParam("month") String months,
                             @FormParam("start-month") String startMonth,
                             @FormParam("end-month") String endMonth,
                             @FormParam("format") String format) {
        return stat(authorization, months, startMonth, endMonth, format);
    }

    public Response stat(String authorization, String monthsParam, String startMonth, String endMonth, String format) {
        if (log.isDebugEnabled())
            log.debug("Attempting to request stat, month: {}, startMonth: {}, endMonth: {}, format: {}",
                    escapeLog(monthsParam), escapeLog(startMonth), escapeLog(endMonth), escapeLog(format));

        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.STAT);
        validateAuthorization(authorization);
        final Set<String> months = validateMonths(monthsParam, startMonth, endMonth);

        try {
            if (log.isTraceEnabled())
                log.trace("Recognized months: {}", escapeLog(months));
            final StatResponse statResponse = statResponseService.buildResponse(months);

            final String responseAsStr;
            if ("openmetrics".equalsIgnoreCase(format)) {
                responseAsStr = createOpenMetricsResponse(statResponse);
            } else if ("jsonmonth".equalsIgnoreCase(format)) {
                responseAsStr = ServerUtil.asJson(statResponse);
            } else {
                responseAsStr = ServerUtil.asJson(new FlatStatResponse(new ArrayList<>(statResponse.getResponse().values())));
            }
            if (log.isTraceEnabled())
                log.trace("Stat: {}", responseAsStr);
            return Response.ok().entity(responseAsStr).build();
        } catch (WebApplicationException e) {
            if (log.isErrorEnabled())
                log.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
    }

    private void validateAuthorization(String authorization) {
        log.trace("Validating authorization: {}", authorization);

        AuthorizationGrant grant = tokenService.getAuthorizationGrant(authorization);
        if (grant == null) {
            log.trace("Unable to find token by authorization: {}", authorization);
            throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, TokenErrorResponseType.ACCESS_DENIED, "Can't find grant for authorization.");
        }

        final AbstractToken accessToken = grant.getAccessToken(tokenService.getToken(authorization));
        if (accessToken == null) {
            log.trace("Unable to find token by authorization: {}", authorization);
            throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, TokenErrorResponseType.ACCESS_DENIED, "Can't find access token.");
        }

        if (accessToken.isExpired()) {
            log.trace("Access Token is expired: {}", accessToken.getCode());
            throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, TokenErrorResponseType.ACCESS_DENIED, "Token expired.");
        }

        if (!grant.getScopesAsString().contains(appConfiguration.getStatAuthorizationScope())) {
            log.trace("Access Token does NOT have '{}' scope which is required to call Statistic Endpoint.", appConfiguration.getStatAuthorizationScope());
            throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, TokenErrorResponseType.ACCESS_DENIED, appConfiguration.getStatAuthorizationScope() + " scope is required for token.");
        }
    }

    private Set<String> validateMonths(String months, String startMonth, String endMonth) {
        if (!Months.isValid(months, startMonth, endMonth)) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, TokenErrorResponseType.INVALID_REQUEST, "`month` or `start-month`/`end-month` parameter(s) can't be blank and should be in format yyyyMM (e.g. 202012)");
        }

        months = ServerUtil.urlDecode(months);

        Set<String> monthList = Months.getMonths(months, startMonth, endMonth);

        if (monthList.isEmpty()) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, TokenErrorResponseType.INVALID_REQUEST, "Unable to identify months. Check `month` or `start-month`/`end-month` parameter(s). It can't be blank and should be in format yyyyMM (e.g. 202012). start-month must be before end-month");
        }

        return monthList;
    }
}
