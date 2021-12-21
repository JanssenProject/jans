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
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.util.ServerUtil;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

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

    @POST
    @Path("/active")
    @Produces({MediaType.APPLICATION_JSON})
    public Response requestActiveSessions(
            @HeaderParam("Authorization") String authorization,
            @Context HttpServletRequest request,
            @Context SecurityContext securityContext) {
        try {
            errorResponseFactory.validateComponentEnabled(ComponentType.ACTIVE_SESSION);
            AuthorizationGrant grant = validateToken(getToken(authorization));

            Response.ResponseBuilder builder = Response.ok();
            builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
            builder.header(Constants.PRAGMA, Constants.NO_CACHE);
            builder.entity(createEntity(grant));
            return builder.build();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Object createEntity(AuthorizationGrant grant) {
        return "{}";
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

        if (BooleanUtils.isFalse(appConfiguration.getOpenidScopeBackwardCompatibility()) && !authorizationGrant.getScopes().contains(DefaultScope.OPEN_ID.toString())) {
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
