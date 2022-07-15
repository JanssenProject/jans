package io.jans.as.server.token.ws.rs;

import io.jans.as.model.common.GrantType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.util.ServerUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.tika.utils.StringUtils;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

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
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty();
    }

    public void validateGrantType(GrantType requestedGrantType, GrantType[] clientGrantTypesArray, OAuth2AuditLog auditLog) {
        List<GrantType> clientGrantTypes = Arrays.asList(clientGrantTypesArray);
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

    private Response.ResponseBuilder error(int status, TokenErrorResponseType type, String reason) {
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(errorResponseFactory.errorAsJson(type, reason));
    }

}
