package io.jans.as.server.token.ws.rs;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.DeviceAuthorizationCacheControl;
import io.jans.as.server.model.common.RefreshToken;
import io.jans.as.server.util.ServerUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static io.jans.as.model.config.Constants.REASON_CLIENT_NOT_AUTHORIZED;

/**
 * @author Yuriy Zabrovarnyy
 */
@Named
@Stateless
public class TokenRestWebServiceValidator {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private AppConfiguration appConfiguration;

    public void validateParams(String grantType, String code,
                               String redirectUri, String refreshToken, OAuth2AuditLog auditLog) {
        log.debug("Starting to validate request parameters");
        if (grantType == null || grantType.isEmpty()) {
            final String msg = "Grant Type is not set.";
            log.trace(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), auditLog));
        }

        GrantType gt = GrantType.fromString(grantType);

        if (gt == GrantType.AUTHORIZATION_CODE) {
            if (StringUtils.isBlank(code)) {
                final String msg = "Code is not set for AUTHORIZATION_CODE.";
                log.trace(msg);
                throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), auditLog));
            }
            if (StringUtils.isBlank(redirectUri)) {
                final String msg = "redirect_uri is not set for AUTHORIZATION_CODE.";
                log.trace(msg);
                throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), auditLog));
            }
            return;
        }

        if (gt == GrantType.REFRESH_TOKEN && StringUtils.isBlank(refreshToken)) {
            final String msg = "Refresh Token is not set for REFRESH_TOKEN.";
            log.trace(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), auditLog));
        }
    }

    public static boolean validateParams(String clientId, String clientSecret) {
        return StringUtils.isNotBlank(clientId) && StringUtils.isNotBlank(clientSecret);
    }

    public void validateGrantType(GrantType requestedGrantType, Client client, OAuth2AuditLog auditLog) {
        List<GrantType> clientGrantTypes = Arrays.asList(client.getGrantTypes());
        if (!clientGrantTypes.contains(requestedGrantType)) {
            final String msg = "GrantType is not allowed by client's grantTypes.";
            log.trace(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, msg), auditLog));
        }

        if (!appConfiguration.getGrantTypesSupported().contains(requestedGrantType)) {
            final String msg = "GrantType is not allowed by AS configuration";
            log.trace(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, msg), auditLog));
        }
    }

    private Response response(Response.ResponseBuilder builder, OAuth2AuditLog oAuth2AuditLog) {
        builder.cacheControl(ServerUtil.cacheControl(true, false));
        builder.header("Pragma", "no-cache");

        applicationAuditLogger.sendMessage(oAuth2AuditLog);

        return builder.build();
    }

    public Response.ResponseBuilder error(int status, TokenErrorResponseType type, String reason) {
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(errorResponseFactory.errorAsJson(type, reason));
    }

    @NotNull
    public Client validateClient(Client client, OAuth2AuditLog auditLog) {
        if (client == null) {
            throw new WebApplicationException(response(error(Response.Status.UNAUTHORIZED.getStatusCode(), TokenErrorResponseType.INVALID_GRANT, "Unable to find client."), auditLog));
        }

        log.debug("Get client from session: '{}'", client.getClientId());
        if (client.isDisabled()) {
            throw new WebApplicationException(response(error(Response.Status.FORBIDDEN.getStatusCode(), TokenErrorResponseType.DISABLED_CLIENT, "Client is disabled."), auditLog));
        }
        return client;
    }


    public void validateDeviceAuthorization(Client client, String deviceCode, DeviceAuthorizationCacheControl cacheData, OAuth2AuditLog oAuth2AuditLog) {
        if (cacheData == null) {
            log.debug("The authentication request has expired for deviceCode: '{}'", deviceCode);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.EXPIRED_TOKEN, "The authentication request has expired."), oAuth2AuditLog));
        }
        if (!cacheData.getClient().getClientId().equals(client.getClientId())) {
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, REASON_CLIENT_NOT_AUTHORIZED), oAuth2AuditLog));
        }
    }

    public void validateGrant(AuthorizationGrant grant, Client client, Object identifier, OAuth2AuditLog auditLog) {
        validateGrant(grant, client, identifier, auditLog, null);
    }


    public void validateGrant(AuthorizationGrant grant, Client client, Object identifier, OAuth2AuditLog auditLog, Consumer<AuthorizationGrant> onFailure) {
        if (grant == null) {
            log.debug("AuthorizationGrant not found by clientId: '{}', identifier: '{}'", client.getClientId(), identifier);
            if (onFailure != null) {
                onFailure.accept(grant);
            }
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Unable to find grant object for given code."), auditLog));
        }

        if (!client.getClientId().equals(grant.getClientId())) {
            log.debug("AuthorizationGrant is found but belongs to another client. Grant's clientId: '{}', identifier: '{}'", grant.getClientId(), identifier);
            if (onFailure != null) {
                onFailure.accept(grant);
            }
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Client mismatch."), auditLog));
        }
    }

    public void validateRefreshToken(RefreshToken refreshTokenObject, OAuth2AuditLog auditLog) {
        if (refreshTokenObject == null || !refreshTokenObject.isValid()) {
            log.trace("Invalid refresh token.");
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Unable to find refresh token or otherwise token type or client does not match."), auditLog));
        }
    }

    public void validateUser(User user, OAuth2AuditLog auditLog) {
        if (user == null) {
            log.debug("Invalid user", new RuntimeException("User is empty"));
            throw new WebApplicationException(response(error(401, TokenErrorResponseType.INVALID_CLIENT, "Invalid user."), auditLog));
        }
    }
}
