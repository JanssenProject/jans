/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.revoke;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.common.TokenTypeHint;
import org.gluu.oxauth.model.token.TokenRevocationErrorResponseType;
import org.slf4j.Logger;
import org.xdi.oxauth.audit.ApplicationAuditLogger;
import org.xdi.oxauth.model.audit.Action;
import org.xdi.oxauth.model.audit.OAuth2AuditLog;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.session.SessionClient;
import org.xdi.oxauth.security.Identity;
import org.xdi.oxauth.service.GrantService;
import org.xdi.oxauth.util.ServerUtil;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * Provides interface for token revocation REST web services
 *
 * @author Javier Rojas Blum
 * @version January 16, 2019
 */
@Path("/")
public class RevokeRestWebServiceImpl implements RevokeRestWebService {

    @Inject
    private Logger log;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private Identity identity;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private GrantService grantService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Override
    public Response requestAccessToken(String token, String tokenTypeHint,
                                       HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
        log.debug(
                "Attempting to revoke token: token = {}, tokenTypeHint = {}, isSecure = {}",
                token, tokenTypeHint, sec.isSecure());
        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(request), Action.TOKEN_REVOCATION);

        Response.ResponseBuilder builder = Response.ok();
        SessionClient sessionClient = identity.getSessionClient();
        Client client = null;
        if (sessionClient != null) {
            client = sessionClient.getClient();
            oAuth2AuditLog.setClientId(client.getClientId());

            if (validateToken(token)) {
                TokenTypeHint tth = TokenTypeHint.getByValue(tokenTypeHint);
                AuthorizationGrant authorizationGrant = null;

                if (tth == TokenTypeHint.ACCESS_TOKEN) {
                    authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(token);
                } else if (tth == TokenTypeHint.REFRESH_TOKEN) {
                    authorizationGrant = authorizationGrantList.getAuthorizationGrantByRefreshToken(client.getClientId(), token);
                } else {
                    // Since the hint about the type of the token submitted for revocation is optional. oxAuth will
                    // search it as Access Token then as Refresh Token.
                    authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(token);
                    if (authorizationGrant == null) {
                        authorizationGrant = authorizationGrantList.getAuthorizationGrantByRefreshToken(client.getClientId(), token);
                    }
                }

                if (authorizationGrant != null) {
                    grantService.removeAllTokensBySession(authorizationGrant.getTokenLdap().getSessionDn());
                }
            } else {
                builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode()); // 400
                builder.entity(errorResponseFactory.getErrorAsJson(
                        TokenRevocationErrorResponseType.INVALID_REQUEST));
            }
        }

        return response(builder, oAuth2AuditLog);
    }

    private boolean validateToken(String token) {
        return StringUtils.isNotBlank(token);
    }

    private TokenTypeHint validateTokenTypeHint(String tokenTypeHint) {
        return TokenTypeHint.getByValue(tokenTypeHint);
    }

    private Response response(Response.ResponseBuilder builder, OAuth2AuditLog oAuth2AuditLog) {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoTransform(false);
        cacheControl.setNoStore(true);
        builder.cacheControl(cacheControl);
        builder.header("Pragma", "no-cache");

        applicationAuditLogger.sendMessage(oAuth2AuditLog);

        return builder.build();
    }
}
