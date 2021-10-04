/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.revoke;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.ComponentType;
import io.jans.as.model.common.TokenTypeHint;
import io.jans.as.model.config.Constants;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.TokenRevocationErrorResponseType;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.session.SessionClient;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.GrantService;
import io.jans.as.server.service.external.ExternalRevokeTokenService;
import io.jans.as.server.service.external.context.RevokeTokenContext;
import io.jans.as.server.util.ServerUtil;
import org.apache.commons.lang.StringUtils;
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

    @Inject
    private ExternalRevokeTokenService externalRevokeTokenService;

    @Override
    public Response requestAccessToken(String token, String tokenTypeHint, String clientId,
                                       HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
        log.debug("Attempting to revoke token: token = {}, tokenTypeHint = {}, isSecure = {}", token, tokenTypeHint, sec.isSecure());
        errorResponseFactory.validateComponentEnabled(ComponentType.REVOKE_TOKEN);
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
            // Since the hint about the type of the token submitted for revocation is optional. Jans Auth will
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

        RevokeTokenContext revokeTokenContext = new RevokeTokenContext(request, client, authorizationGrant, builder);
        final boolean scriptResult = externalRevokeTokenService.revokeTokenMethods(revokeTokenContext);
        if (!scriptResult) {
            log.trace("Revoke is forbidden by 'Revoke Token' custom script (method returned false). Exit without revoking.");
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
        builder.header(Constants.PRAGMA, Constants.NO_CACHE);

        applicationAuditLogger.sendMessage(oAuth2AuditLog);

        return builder.build();
    }
}
