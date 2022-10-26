/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.revoke;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.common.TokenTypeHint;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.TokenRevocationErrorResponseType;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.model.ldap.TokenEntity;
import io.jans.as.server.model.ldap.TokenType;
import io.jans.as.server.model.session.SessionClient;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.GrantService;
import io.jans.as.server.service.external.ExternalRevokeTokenService;
import io.jans.as.server.util.ServerUtil;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;

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

    @Inject
    private AppConfiguration appConfiguration;

    @Override
    public Response requestAccessToken(String tokenString, String tokenTypeHint, String clientId,
                                       HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
        log.debug("Attempting to revoke token: token = {}, tokenTypeHint = {}, isSecure = {}", tokenString, tokenTypeHint, sec.isSecure());
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.REVOKE_TOKEN);
        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(request), Action.TOKEN_REVOCATION);

        validateToken(tokenString);

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

        ExecutionContext executionContext = new ExecutionContext(request, response);
        executionContext.setClient(client);
        executionContext.setResponseBuilder(builder);

        final boolean scriptResult = externalRevokeTokenService.revokeTokenMethods(executionContext);
        if (!scriptResult) {
            log.trace("Revoke is forbidden by 'Revoke Token' custom script (method returned false). Exit without revoking.");
            return response(builder, oAuth2AuditLog);
        }

        TokenTypeHint tth = TokenTypeHint.getByValue(tokenTypeHint);
        boolean isAll = Constants.ALL.equalsIgnoreCase(tokenString) && appConfiguration.getAllowAllValueForRevokeEndpoint();
        if (isAll) {
            removeAllTokens(tth, executionContext);
            return response(builder, oAuth2AuditLog);
        }

        String[] tokens = tokenString.split(" ");
        if (ArrayUtils.isEmpty(tokens)) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST.getStatusCode())
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(errorResponseFactory.errorAsJson(TokenRevocationErrorResponseType.INVALID_REQUEST, "Failed to validate token."))
                    .build());
        }

        boolean isSingle = tokens.length == 1;
        for (String token : tokens) {
            final Response removeTokenResponse = removeToken(token, executionContext, tth, oAuth2AuditLog, isSingle);
            if (removeTokenResponse != null) {
                return removeTokenResponse;
            }
        }

        return response(builder, oAuth2AuditLog);
    }

    private Response removeToken(String token, ExecutionContext executionContext, TokenTypeHint tth, OAuth2AuditLog oAuth2AuditLog, boolean single) {
        final Client client = executionContext.getClient();
        AuthorizationGrant authorizationGrant = findAuthorizationGrant(client, token, tth);

        if (authorizationGrant == null && !single) {
            log.trace("Unable to find token.");
            return response(executionContext.getResponseBuilder(), oAuth2AuditLog);
        }

        if (!single && !authorizationGrant.getClientId().equals(client.getClientId())) {
            log.trace("Token was issued with client {} but revoke is requested with client {}. Skip revoking.", authorizationGrant.getClientId(), client.getClientId());
            return response(executionContext.getResponseBuilder(), oAuth2AuditLog);
        }

        if (authorizationGrant != null) {
            grantService.removeAllByGrantId(authorizationGrant.getGrantId());
            log.trace("Revoked successfully token {}", token);
        }

        return null;
    }

    private void removeAllTokens(TokenTypeHint tth, ExecutionContext executionContext) {
        final List<TokenEntity> tokens = grantService.getGrantsOfClient(executionContext.getClient().getClientId());
        for (TokenEntity token : tokens) {
            if (tth == null ||
                    (tth == TokenTypeHint.ACCESS_TOKEN && token.getTokenTypeEnum() == TokenType.ACCESS_TOKEN) ||
                    (tth == TokenTypeHint.REFRESH_TOKEN && token.getTokenTypeEnum() == TokenType.REFRESH_TOKEN)) {
                grantService.removeSilently(token);
            }
        }
    }

    private AuthorizationGrant findAuthorizationGrant(Client client, String token, TokenTypeHint tth) {
        if (tth == TokenTypeHint.ACCESS_TOKEN) {
            return authorizationGrantList.getAuthorizationGrantByAccessToken(token);
        } else if (tth == TokenTypeHint.REFRESH_TOKEN) {
            return authorizationGrantList.getAuthorizationGrantByRefreshToken(client.getClientId(), token);
        } else {
            // Since the hint about the type of the token submitted for revocation is optional. Jans Auth will
            // search it as Access Token then as Refresh Token.
            AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(token);
            if (authorizationGrant == null) {
                authorizationGrant = authorizationGrantList.getAuthorizationGrantByRefreshToken(client.getClientId(), token);
            }
            return authorizationGrant;
        }
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
