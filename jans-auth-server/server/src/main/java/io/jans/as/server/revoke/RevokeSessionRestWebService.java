/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.revoke;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.session.EndSessionErrorResponseType;
import io.jans.as.server.model.config.Constants;
import io.jans.as.server.model.session.SessionClient;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.ScopeService;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.UserService;
import io.jans.as.server.service.session.SessionStatusListIndexService;
import io.jans.model.tokenstatus.TokenStatus;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


/**
 * @author Yuriy Zabrovarnyy
 */
@Path("/")
public class RevokeSessionRestWebService {

    private static final ExecutorService sessionStatusListPool = Executors.newFixedThreadPool(5, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("session_status_list_pool");
        thread.setDaemon(true);
        return thread;
    });

    @Inject
    private Logger log;

    @Inject
    private UserService userService;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private Identity identity;

    @Inject
    private ScopeService scopeService;

    @Inject
    private SessionStatusListIndexService sessionStatusListIndexService;

    @POST
    @Path("/revoke_session")
    @Produces({MediaType.APPLICATION_JSON})
    public Response requestRevokeSession(
            @FormParam("user_criterion_key") String userCriterionKey,
            @FormParam("user_criterion_value") String userCriterionValue,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @Context SecurityContext sec) {
        try {
            log.debug("Attempting to revoke session: userCriterionKey = {}, userCriterionValue = {}, isSecure = {}",
                    userCriterionKey, userCriterionValue, sec.isSecure());

            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.REVOKE_SESSION);
            validateAccess();

            final User user = userService.getUserByAttribute(userCriterionKey, userCriterionValue);
            if (user == null) {
                log.trace("Unable to find user by {}={}", userCriterionKey, userCriterionValue);
                return Response.ok().build(); // no error because we don't want to disclose internal AS info about users
            }

            List<SessionId> sessionIdList = sessionIdService.findByUser(user.getDn());
            if (sessionIdList == null || sessionIdList.isEmpty()) {
                log.trace("No sessions found for user uid: {}, dn: {}", user.getUserId(), user.getDn());
                return Response.ok().build();
            }

            final List<SessionId> authenticatedSessions = sessionIdList.stream().filter(sessionId -> sessionId.getState() == SessionIdState.AUTHENTICATED).collect(Collectors.toList());
            sessionIdService.remove(authenticatedSessions);

            sessionStatusListPool.execute(() -> {
                final List<Integer> indexes = authenticatedSessions.stream().map(sessionId -> sessionId.getPredefinedAttributes().getIndex()).collect(Collectors.toList());
                sessionStatusListIndexService.updateStatusAtIndexes(indexes, TokenStatus.INVALID);
            });
            log.debug("Revoked {} user's sessions (user: {})", authenticatedSessions.size(), user.getUserId());

            return Response.ok().build();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(500).build();
        }
    }

    private void validateAccess() {
        SessionClient sessionClient = identity.getSessionClient();
        if (sessionClient == null || sessionClient.getClient() == null || ArrayUtils.isEmpty(sessionClient.getClient().getScopes())) {
            log.debug("Client failed to authenticate.");
            throw new WebApplicationException(
                    Response.status(Response.Status.UNAUTHORIZED.getStatusCode())
                            .entity(errorResponseFactory.getErrorAsJson(EndSessionErrorResponseType.INVALID_REQUEST))
                            .build());
        }

        List<String> scopesAllowedIds = scopeService.getScopeIdsByDns(Arrays.asList(sessionClient.getClient().getScopes()));

        if (!scopesAllowedIds.contains(Constants.REVOKE_SESSION_SCOPE)) {
            log.debug("Client does not have required revoke_session scope.");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED.getStatusCode())
                    .entity(errorResponseFactory.getErrorAsJson(EndSessionErrorResponseType.INVALID_REQUEST))
                    .build());
        }
    }
}
