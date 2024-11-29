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
import io.jans.as.server.model.session.SessionClient;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.GrantService;
import io.jans.as.server.service.external.ExternalRevokeTokenService;
import io.jans.as.server.util.ServerUtil;
import io.jans.model.token.TokenEntity;
import io.jans.model.token.TokenType;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.jans.as.server.model.config.Constants.REVOKE_ANY_TOKEN_SCOPE;

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
                    .entity(errorResponseFactory.errorAsJson(TokenRevocationErrorResponseType.INVALID_REQUEST, "Failed to validate token"))
                    .build());
        }

        for (String token : tokens) {
            removeToken(token, executionContext, tth);
        }

        return response(builder, oAuth2AuditLog);
    }

    private void removeToken(String token, ExecutionContext executionContext, TokenTypeHint tth) {
        final Client client = executionContext.getClient();
        AuthorizationGrant authorizationGrant = findAuthorizationGrant(token, tth);
        if (log.isTraceEnabled()) {
            String msg = authorizationGrant != null ? authorizationGrant.getGrantId() : "not";
            log.trace("Grant {} found for token {}", msg, token);
        }

        if (authorizationGrant == null) {
            log.trace("Unable to find grant for token {}", token);
            return;
        }

        validateSameClient(authorizationGrant, client);
        validateScope(authorizationGrant, client);

        grantService.removeAllByGrantId(authorizationGrant.getGrantId());
        log.trace("Revoked successfully token {}", token);
    }

    public void validateScope(AuthorizationGrant authorizationGrant, Client client) {
        if (authorizationGrant.getClientId().equals(client.getClientId())) {
            return; // client owns this client -> nothing to do
        }

        if (client.getScopes() != null && Arrays.asList(client.getScopes()).contains(REVOKE_ANY_TOKEN_SCOPE)) {
            return; // client has 'revoke_any_token' scope
        }

        log.trace("Client {} does not have 'revoke_any_token' scope which is required to be able revoke other client's tokens", client.getClientId());
        throw new WebApplicationException(Response
                .status(Response.Status.BAD_REQUEST.getStatusCode())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(errorResponseFactory.errorAsJson(TokenRevocationErrorResponseType.INVALID_REQUEST, "Client does not have 'revoke_any_token' scope"))
                .build());
    }

    public void validateSameClient(AuthorizationGrant grant, Client client) {
        if (!grant.getClientId().equals(client.getClientId()) && BooleanUtils.isFalse(appConfiguration.getAllowRevokeForOtherClients())) {
            log.trace("Token was issued with client {} but revoke is requested with client {}. Skip revoking.", grant.getClientId(), client.getClientId());
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST.getStatusCode())
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(errorResponseFactory.errorAsJson(TokenRevocationErrorResponseType.INVALID_REQUEST, "Failed to validate token."))
                    .build());
        }
    }

    private void removeAllTokens(TokenTypeHint tth, ExecutionContext executionContext) {
        final String clientId = executionContext.getClient().getClientId();
        final List<TokenEntity> tokens = grantService.getGrantsOfClient(clientId);
        log.debug("Revoking all tokens of client {}...", clientId);

        List<TokenEntity> tokensToRemove = new ArrayList<>();
        for (TokenEntity token : tokens) {
            if (tth == null ||
                    (tth == TokenTypeHint.ACCESS_TOKEN && token.getTokenTypeEnum() == TokenType.ACCESS_TOKEN) ||
                    (tth == TokenTypeHint.TX_TOKEN && token.getTokenTypeEnum() == TokenType.TX_TOKEN) ||
                    (tth == TokenTypeHint.REFRESH_TOKEN && token.getTokenTypeEnum() == TokenType.REFRESH_TOKEN)) {
               tokensToRemove.add(token);
            }
        }
        grantService.removeSilently(tokensToRemove);
        log.debug("Revoked all tokens of client {}.", clientId);
    }

    private AuthorizationGrant findAuthorizationGrant(String token, TokenTypeHint tth) {
        if (tth == TokenTypeHint.ACCESS_TOKEN || tth == TokenTypeHint.TX_TOKEN) {
            return authorizationGrantList.getAuthorizationGrantByAccessToken(token);
        }

        final TokenEntity grantByCode = grantService.getGrantByCode(token);
        return authorizationGrantList.asGrant(grantByCode);
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
