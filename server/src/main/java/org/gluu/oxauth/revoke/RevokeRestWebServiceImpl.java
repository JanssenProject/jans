/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package org.gluu.oxauth.revoke;

import io.jans.as.model.common.TokenTypeHint;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.TokenRevocationErrorResponseType;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.audit.ApplicationAuditLogger;
import org.gluu.oxauth.model.audit.Action;
import org.gluu.oxauth.model.audit.OAuth2AuditLog;
import org.gluu.oxauth.model.common.AuthorizationGrant;
import org.gluu.oxauth.model.common.AuthorizationGrantList;
import io.jans.as.common.model.registration.Client;
import org.gluu.oxauth.model.session.SessionClient;
import org.gluu.oxauth.security.Identity;
import org.gluu.oxauth.service.ClientService;
import org.gluu.oxauth.service.GrantService;
import org.gluu.oxauth.util.ServerUtil;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * Provides interface for token revocation REST web services
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
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

    @Inject
    private ClientService clientService;

    @Override
    public Response requestAccessToken(String token, String tokenTypeHint, String clientId,
                                       HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
        log.debug("Attempting to revoke token: token = {}, tokenTypeHint = {}, isSecure = {}", token, tokenTypeHint, sec.isSecure());
        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(request), Action.TOKEN_REVOCATION);

        validateToken(token);

        Response.ResponseBuilder builder = Response.ok();
        SessionClient sessionClient = identity.getSessionClient();

        Client client = sessionClient != null ? sessionClient.getClient() : null;
        if (client == null) {
            client = clientService.getClient(clientId);
            if (!clientService.isPublic(client)) {
                log.trace("Client is not public and not authenticated. Skip revoking.");
                return response(builder, oAuth2AuditLog);
            }
        }
        if (client == null) {
            log.trace("Client is not unknown. Skip revoking.");
            return response(builder, oAuth2AuditLog);
        }

        oAuth2AuditLog.setClientId(client.getClientId());

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

        if (authorizationGrant == null) {
            log.trace("Unable to find token.");
            return response(builder, oAuth2AuditLog);
        }
        if (!authorizationGrant.getClientId().equals(client.getClientId())) {
            log.trace("Token was issued with client {} but revoke is requested with client {}. Skip revoking.", authorizationGrant.getClientId(), client.getClientId());
            return response(builder, oAuth2AuditLog);
        }

        grantService.removeAllByGrantId(authorizationGrant.getGrantId());
        log.trace("Revoked successfully.");

        return response(builder, oAuth2AuditLog);
    }

    private void validateToken(String token) {
        if (StringUtils.isBlank(token)) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST.getStatusCode())
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(errorResponseFactory.errorAsJson(TokenRevocationErrorResponseType.INVALID_REQUEST, "Failed to validate token."))
                    .build());
        }
    }

    private Response response(Response.ResponseBuilder builder, OAuth2AuditLog oAuth2AuditLog) {
        builder.cacheControl(ServerUtil.cacheControl(true, false));
        builder.header("Pragma", "no-cache");

        applicationAuditLogger.sendMessage(oAuth2AuditLog);

        return builder.build();
    }
}
