package io.jans.as.server.ws.rs.stat;

import io.jans.as.common.model.stat.StatEntry;
import io.jans.as.model.common.ComponentType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.server.model.common.AbstractToken;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.service.stat.StatService;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.util.ServerUtil;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.common.TextFormat;
import net.agkn.hll.HLL;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

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

    private static final int DEFAULT_WS_INTERVAL_LIMIT_IN_SECONDS = 60;

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private StatService statService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private TokenService tokenService;

    private long lastProcessedAt;

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
    public Response statGet(@HeaderParam("Authorization") String authorization, @QueryParam("month") String month, @QueryParam("format") String format) {
        return stat(authorization, month, format);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response statPost(@HeaderParam("Authorization") String authorization, @FormParam("month") String month, @FormParam("format") String format) {
        return stat(authorization, month, format);
    }

    public Response stat(String authorization, String month, String format) {
        log.debug("Attempting to request stat, month: {}, format: {}", month, format);

        errorResponseFactory.validateComponentEnabled(ComponentType.STAT);
        validateAuthorization(authorization);
        final List<String> months = validateMonth(month);

        if (!allowToRun()) {
            log.trace("Interval request limit exceeded. Request is rejected. Current interval limit: {} (or 60 seconds if not set).", appConfiguration.getStatWebServiceIntervalLimitInSeconds());
            throw errorResponseFactory.createWebApplicationException(Response.Status.FORBIDDEN, TokenErrorResponseType.ACCESS_DENIED, "Interval request limit exceeded.");
        }

        lastProcessedAt = System.currentTimeMillis();

        try {
            log.trace("Recognized months: {}", months);
            final StatResponse statResponse = buildResponse(months);

            final String responseAsStr;
            if ("openmetrics".equalsIgnoreCase(format)) {
                responseAsStr = createOpenMetricsResponse(statResponse);
            } else if ("jsonmonth".equalsIgnoreCase(format)) {
                responseAsStr = ServerUtil.asJson(statResponse);
            } else {
                responseAsStr = ServerUtil.asJson(new FlatStatResponse(new ArrayList<>(statResponse.getResponse().values())));
            }
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

    private StatResponse buildResponse(List<String> months) {
        StatResponse response = new StatResponse();
        for (String month : months) {
            final StatResponseItem responseItem = buildItem(month);
            if (responseItem != null) {
                response.getResponse().put(month, responseItem);
            }
        }

        return response;
    }

    private StatResponseItem buildItem(String month) {
        try {
            String monthlyDn = String.format("ou=%s,%s", month, statService.getBaseDn());

            final List<StatEntry> entries = entryManager.findEntries(monthlyDn, StatEntry.class, Filter.createPresenceFilter("jansId"));
            if (entries == null || entries.isEmpty()) {
                log.trace("Can't find stat entries for month: {}", monthlyDn);
                return null;
            }

            final StatResponseItem responseItem = new StatResponseItem();
            responseItem.setMonthlyActiveUsers(userCardinality(entries));
            responseItem.setMonth(month);

            unionTokenMapIntoResponseItem(entries, responseItem);

            return responseItem;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private void unionTokenMapIntoResponseItem(List<StatEntry> entries, StatResponseItem responseItem) {
        for (StatEntry entry : entries) {
            entry.getStat().getTokenCountPerGrantType().entrySet().stream().filter(en -> en.getValue() != null).forEach(en -> {
                final Map<String, Long> tokenMap = responseItem.getTokenCountPerGrantType().get(en.getKey());
                if (tokenMap == null) {
                    responseItem.getTokenCountPerGrantType().put(en.getKey(), en.getValue());
                    return;
                }
                for (Map.Entry<String, Long> tokenEntry : en.getValue().entrySet()) {
                    final Long counter = tokenMap.get(tokenEntry.getKey());
                    if (counter == null) {
                        tokenMap.put(tokenEntry.getKey(), tokenEntry.getValue());
                        continue;
                    }

                    tokenMap.put(tokenEntry.getKey(), counter + tokenEntry.getValue());
                }
            });
        }
    }

    private long userCardinality(List<StatEntry> entries) {
        HLL hll = decodeHll(entries.get(0));

        // Union hll
        if (entries.size() > 1) {
            for (int i = 1; i < entries.size(); i++) {
                hll.union(decodeHll(entries.get(i)));
            }
        }
        return hll.cardinality();
    }

    private HLL decodeHll(StatEntry entry) {
        try {
            return HLL.fromBytes(Base64.getDecoder().decode(entry.getUserHllData()));
        } catch (Exception e) {
            log.error("Failed to decode HLL data, entry dn: {}, data: {}", entry.getDn(), entry.getUserHllData());
            return statService.newHll();
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

    private List<String> validateMonth(String month) {
        if (StringUtils.isBlank(month)) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, TokenErrorResponseType.INVALID_REQUEST, "`month` parameter can't be blank and should be in format yyyyMM (e.g. 202012)");
        }

        month = ServerUtil.urlDecode(month);

        List<String> months = new ArrayList<>();
        for (String m : month.split(" ")) {
            m = m.trim();
            if (m.length() == 6) {
                months.add(m);
            }
        }

        if (months.isEmpty()) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, TokenErrorResponseType.INVALID_REQUEST, "`month` parameter can't be blank and should be in format yyyyMM (e.g. 202012)");
        }

        return months;
    }

    private boolean allowToRun() {
        int interval = appConfiguration.getStatWebServiceIntervalLimitInSeconds();
        if (interval <= 0) {
            interval = DEFAULT_WS_INTERVAL_LIMIT_IN_SECONDS;
        }

        long timerInterval = interval * 1000L;

        long timeDiff = System.currentTimeMillis() - lastProcessedAt;

        return timeDiff >= timerInterval;
    }
}
