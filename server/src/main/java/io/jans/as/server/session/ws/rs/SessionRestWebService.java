package io.jans.as.server.session.ws.rs;

import io.jans.as.model.common.ComponentType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.userinfo.UserInfoErrorResponseType;
import io.jans.as.server.model.common.AbstractToken;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.common.DefaultScope;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.model.common.SessionId;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.util.ServerUtil;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

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

    @POST
    @Path("/active")
    @Produces({MediaType.APPLICATION_JSON})
    public Response requestActiveSessions(
            @HeaderParam("Authorization") String authorization,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @Context SecurityContext securityContext) {
        try {
            errorResponseFactory.validateComponentEnabled(ComponentType.ACTIVE_SESSION);
            AuthorizationGrant grant = validateToken(getToken(authorization));

            ExecutionContext executionContext = new ExecutionContext(request, response);
            executionContext.setGrant(grant);

            List<SessionId> sessionIdList = getUserSessions(grant);
            executionContext.setUserSessions(sessionIdList);

            JSONArray jsonArray = createJsonArray(executionContext);

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

        JSONObject result = new JSONObject();
        if (lastUsedAt != null)
            result.put("lastUsedAt", dateAsSeconds(lastUsedAt));
        if (iat != null)
            result.put("iat", dateAsSeconds(iat));
        if (exp != null)
            result.put("exp", dateAsSeconds(exp));
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
