package io.jans.as.server.session.ws.rs;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.userinfo.UserInfoErrorResponseType;
import io.jans.as.server.model.common.AbstractToken;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.common.DefaultScope;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.external.ExternalApplicationSessionService;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.util.ServerUtil;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static io.jans.as.model.userinfo.UserInfoErrorResponseType.INVALID_TOKEN;

/**
 * @author Yuriy Zabrovarnyy
 */
@Path("/session")
public class SessionRestWebService {

    @Inject
    private Logger log;

    @Inject
    private TokenService tokenService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private ExternalApplicationSessionService externalApplicationSessionService;

    @POST
    @Path("/active")
    @Produces({MediaType.APPLICATION_JSON})
    public Response requestActiveSessions(
            @HeaderParam("Authorization") String authorization,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @Context SecurityContext securityContext) {
        try {
            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.ACTIVE_SESSION);
            AuthorizationGrant grant = validateToken(getToken(authorization));

            ExecutionContext executionContext = new ExecutionContext(request, response);
            executionContext.setGrant(grant);

            List<SessionId> sessionIdList = getUserSessions(grant);
            executionContext.setUserSessions(sessionIdList);

            JSONArray jsonArray = createJsonArray(executionContext);
            if (!externalApplicationSessionService.modifyActiveSessionsResponse(jsonArray, executionContext)) {
                log.trace("Successfully run external modifyActiveSessionsResponse scripts.");
            } else {
                jsonArray = createJsonArray(executionContext);
                log.trace("Canceled changes made by external modifyActiveSessionsResponse script since method returned `false`.");
            }

            return Response.ok()
                    .cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate())
                    .header(Constants.PRAGMA, Constants.NO_CACHE)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(jsonArray.toString())
                    .build();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private List<SessionId> getUserSessions(AuthorizationGrant grant) {
        final String userDn = grant.getUserDn();

        if (StringUtils.isBlank(userDn)) {
            log.warn("User DN is not set in grant object, grant id: {}", grant.getGrantId());
            return new ArrayList<>();
        }
        return sessionIdService.findByUser(grant.getUserDn());
    }

    private JSONArray createJsonArray(ExecutionContext executionContext) {
        final JSONArray result = new JSONArray();

        for (SessionId sessionId : executionContext.getUserSessions()) {
            result.put(createJsonObject(sessionId));
        }

        return result;
    }

    private JSONObject createJsonObject(SessionId sessionId) {
        final Date exp = sessionId.getExpirationDate();
        final Date iat = sessionId.getCreationDate();
        final Date lastUsedAt = sessionId.getLastUsedAt();
        final String sid = sessionId.getOutsideSid();
        final Date authnTime = sessionId.getAuthenticationTime();

        JSONObject result = new JSONObject();
        if (lastUsedAt != null) {
            result.put("last_used_at", dateAsSeconds(lastUsedAt));
        }
        if (iat != null) {
            result.put("iat", dateAsSeconds(iat));
        }
        if (exp != null) {
            result.put("exp", dateAsSeconds(exp));
        }
        if (StringUtils.isNotBlank(sid)) {
            result.put("sid", sid);
        }
        if (authnTime != null) {
            result.put("authn_time", sid);
        }
        return result;
    }

    private static int dateAsSeconds(Date date) {
        if (date == null)
            return -1;
        return (int) (date.getTime() / 1000L);
    }

    private AuthorizationGrant validateToken(String accessToken) {
        if (StringUtils.isBlank(accessToken)) {
            throw new WebApplicationException(response(Response.Status.BAD_REQUEST, INVALID_TOKEN));
        }
        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(accessToken);

        if (authorizationGrant == null) {
            log.trace("Failed to find authorization grant by access_token: {}", accessToken);
            throw new WebApplicationException(response(Response.Status.UNAUTHORIZED, INVALID_TOKEN));
        }

        final AbstractToken accessTokenObject = authorizationGrant.getAccessToken(accessToken);
        if (accessTokenObject == null || !accessTokenObject.isValid()) {
            log.trace("Invalid access token object, access_token: {}, isNull: {}, isValid: {}", accessToken, accessTokenObject == null, false);
            throw new WebApplicationException(response(Response.Status.UNAUTHORIZED, INVALID_TOKEN));
        }

        final Set<String> scopes = authorizationGrant.getScopes();
        if (BooleanUtils.isFalse(appConfiguration.getOpenidScopeBackwardCompatibility()) && !scopes.contains(DefaultScope.OPEN_ID.toString())) {
            throw new WebApplicationException(response(Response.Status.FORBIDDEN, UserInfoErrorResponseType.INSUFFICIENT_SCOPE));
        }

        final String requiredScope = appConfiguration.getActiveSessionAuthorizationScope();
        if (StringUtils.isNotBlank(requiredScope) && !scopes.contains(requiredScope)) {
            log.trace("Required scope {} is not present.", requiredScope);
            throw new WebApplicationException(response(Response.Status.FORBIDDEN, UserInfoErrorResponseType.INSUFFICIENT_SCOPE));
        }

        return authorizationGrant;
    }

    private Response response(Response.Status status, UserInfoErrorResponseType errorResponseType) {
        return Response
                .status(status)
                .entity(errorResponseFactory.errorAsJson(errorResponseType, null))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate())
                .build();
    }

    private String getToken(String authorization) {
        if (tokenService.isBearerAuthToken(authorization)) {
            return tokenService.getBearerToken(authorization);
        }
        return null;
    }
}
