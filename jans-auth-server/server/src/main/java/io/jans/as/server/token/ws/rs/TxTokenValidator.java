package io.jans.as.server.token.ws.rs;

import io.jans.as.model.common.ExchangeTokenType;
import io.jans.as.model.common.SubjectTokenType;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.AbstractToken;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.util.ServerUtil;
import io.jans.as.server.util.TokenHashUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.tika.utils.StringUtils;
import org.slf4j.Logger;

/**
 * @author Yuriy Z
 */
@Stateless
@Named
public class TxTokenValidator {

    @Inject
    private Logger log;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    public void validateRequestedTokenType(String requestedTokenType, OAuth2AuditLog auditLog) {
        if (TxTokenService.isTxTokenFlow(requestedTokenType)) {
            return;
        }

        log.trace("Invalid requested_token_type.");
        throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUESTED_TOKEN_TYPE, "Unknown requested_token_type. For transaction tokens value must be " + ExchangeTokenType.TX_TOKEN.getName()), auditLog));
    }

    public SubjectTokenType validateSubjectTokenType(String subjectTokenType, OAuth2AuditLog auditLog) {
        SubjectTokenType result = SubjectTokenType.fromString(subjectTokenType);
        if (result != null) {
            return result;
        }

        log.trace("Invalid subject_token_type.");
        throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_SUBJECT_TOKEN_TYPE, "Unknown subject_token_type."), auditLog));
    }

    public AuthorizationGrant validateSubjectToken(String subjectToken, SubjectTokenType subjectTokenTypeEnum, OAuth2AuditLog auditLog) {
        if (StringUtils.isBlank(subjectToken)) {
            log.trace("Invalid subject_token. Blank value is not allowed.");
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Invalid subject_token."), auditLog));
        }
        if (subjectTokenTypeEnum == SubjectTokenType.ACCESS_TOKEN) {
            return validateAccessToken(subjectToken, auditLog);
        } else if (subjectTokenTypeEnum == SubjectTokenType.ID_TOKEN) {
            return validateIdToken(subjectToken, auditLog);
        }

        log.trace("Invalid subject_token. subject_token_type is not supported.");
        throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Invalid subject_token. subject_token_type is not supported."), auditLog));
    }

    private AuthorizationGrant validateAccessToken(String subjectToken, OAuth2AuditLog auditLog) {
        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(subjectToken);

        if (authorizationGrant == null) {
            log.trace("Failed to find authorization grant by subject_token: {}", subjectToken);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Failed to find authorization grant by subject_token."),auditLog));
        }

        AbstractToken authorizationAccessToken = authorizationGrant.getAccessToken(subjectToken);
        if ((authorizationAccessToken == null || !authorizationAccessToken.isValid())) {
            log.error("Access token is not valid.");
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Access token is not valid."),auditLog));
        }

        return authorizationGrant;
    }

    private AuthorizationGrant validateIdToken(String subjectToken, OAuth2AuditLog auditLog) {
        try {
            final AuthorizationGrant idTokenGrant = getIdTokenGrant(subjectToken);

            if (idTokenGrant != null) { // id_token is in db
                log.debug("Found subject_token in db.");
                return idTokenGrant;
            }

            final Jwt jwt = Jwt.parse(subjectToken);

            // verify jwt signature if we can't find it in db
            if (!cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), jwt.getHeader().getKeyId(),
                    null, null, jwt.getHeader().getSignatureAlgorithm())) {
                log.error("id_token signature verification failed.");
                throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Invalid subject_token. id_token signature verification failed."), auditLog));
            }

            log.debug("subject_token is validated successfully as id_token.");
            return null;
        } catch (InvalidJwtException e) {
            log.error("Unable to parse subject_token as JWT.", e);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Invalid subject_token. Unable to parse subject_token as JWT."), auditLog));
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unable to validate subject_token as id_token JWT.", e);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Invalid subject_token. Unable to validate subject_token as id_token JWT."), auditLog));
        }
    }

    private Response response(Response.ResponseBuilder builder, OAuth2AuditLog oAuth2AuditLog) {
        builder.cacheControl(ServerUtil.cacheControl(true, false));
        builder.header("Pragma", "no-cache");

        applicationAuditLogger.sendMessage(oAuth2AuditLog);

        return builder.build();
    }

    protected AuthorizationGrant getIdTokenGrant(String idTokenHint) {
        if (org.apache.commons.lang3.StringUtils.isBlank(idTokenHint)) {
            return null;
        }

        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(TokenHashUtil.hash(idTokenHint));
        if (authorizationGrant != null) {
            return authorizationGrant;
        }

        authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
        return authorizationGrant;
    }

    public Response.ResponseBuilder error(int status, TokenErrorResponseType type, String reason) {
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(errorResponseFactory.errorAsJson(type, reason));
    }
}
