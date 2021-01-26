package io.jans.as.server.ws.rs.stat;

import io.jans.as.common.model.stat.StatEntry;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.server.model.session.SessionClient;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.stat.StatService;
import io.jans.as.server.util.ServerUtil;
import io.jans.orm.PersistenceEntryManager;
import net.agkn.hll.HLL;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@Path("/stat")
public class StatWS {

    private static final int DEFAULT_WS_INTERVAL_LIMIT_IN_SECONDS = 60;

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private Identity identity;

    @Inject
    private StatService statService;

    @Inject
    private AppConfiguration appConfiguration;

    private long lastProcessedAt;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response statGet(@HeaderParam("Authorization") String authorization, @QueryParam("month") String month) {
        return stat(month);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response statPost(@HeaderParam("Authorization") String authorization, @FormParam("month") String month) {
        return stat(month);
    }

    public Response stat(String month) {
        return null;
    }

    private long userCardinality(List<StatEntry> entries) {
        final StatEntry firstEntry = entries.get(0);
        HLL hll = HLL.fromBytes(firstEntry.getUserHllData().getBytes(StandardCharsets.UTF_8));

        // Union hll
        if (entries.size() > 1) {
            for (int i = 1; i < entries.size(); i++) {
                hll.union(HLL.fromBytes(entries.get(i).getUserHllData().getBytes(StandardCharsets.UTF_8)));
            }
        }
        return hll.cardinality();
    }

    private void validateAuthorization() {
        SessionClient sessionClient = identity.getSessionClient();
        if (sessionClient == null || sessionClient.getClient() == null) {
            log.trace("Client is not unknown. Skip stat processing.");
            throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, TokenErrorResponseType.INVALID_CLIENT, "Failed to authenticate client.");
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

        long timerInterval = interval * 1000;

        long timeDiff = System.currentTimeMillis() - lastProcessedAt;

        return timeDiff >= timerInterval;
    }
}
