package io.jans.as.server.token.ws.rs;

import io.jans.as.model.common.ExchangeTokenType;
import io.jans.as.model.common.SubjectTokenType;
import io.jans.as.model.configuration.AppConfiguration;
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
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.util.ServerUtil;
import io.jans.as.server.util.TokenHashUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.tika.utils.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * @author Yuriy Z
 */
@Stateless
@Named
public class TxTokenService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    public Response processTxToken(ExecutionContext executionContext) {
        final JSONObject responseJson = process(executionContext);
        final String entity = responseJson.toString();
        return response(Response.ok().entity(entity), executionContext.getAuditLog());
    }

    private JSONObject process(ExecutionContext executionContext) {
        final String requestedTokenType = executionContext.getHttpRequest().getParameter("requested_token_type");
        final String subjectToken = executionContext.getHttpRequest().getParameter("subject_token");
        final String subjectTokenType = executionContext.getHttpRequest().getParameter("subject_token_type");
        final String audience = executionContext.getHttpRequest().getParameter("audience");
        final String requestContext = executionContext.getHttpRequest().getParameter("rctx");

        validateRequestedTokenType(requestedTokenType, executionContext.getAuditLog());
        SubjectTokenType subjectTokenTypeEnum = validateSubjectTokenType(subjectTokenType, executionContext.getAuditLog());
        validateSubjectToken(subjectToken, subjectTokenTypeEnum, executionContext.getAuditLog());

        JSONObject responseJson = new JSONObject();
        return responseJson;
    }

    private Response response(Response.ResponseBuilder builder, OAuth2AuditLog oAuth2AuditLog) {
        builder.cacheControl(ServerUtil.cacheControl(true, false));
        builder.header("Pragma", "no-cache");

        applicationAuditLogger.sendMessage(oAuth2AuditLog);

        return builder.build();
    }

    public void validateRequestedTokenType(String requestedTokenType, OAuth2AuditLog auditLog) {
        if (isTxTokenFlow(requestedTokenType)) {
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

    private void validateSubjectToken(String subjectToken, SubjectTokenType subjectTokenTypeEnum, OAuth2AuditLog auditLog) {
        if (StringUtils.isBlank(subjectToken)) {
            log.trace("Invalid subject_token. Blank value is not allowed.");
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Invalid subject_token."), auditLog));
        }
        if (subjectTokenTypeEnum == SubjectTokenType.ACCESS_TOKEN) {
            validateAccessToken(subjectToken, auditLog);
        } else if (subjectTokenTypeEnum == SubjectTokenType.ID_TOKEN) {
            validateIdToken(subjectToken, auditLog);
        }

        log.trace("Invalid subject_token. subject_token_type is not supported.");
        throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Invalid subject_token. subject_token_type is not supported."), auditLog));
    }

    private void validateAccessToken(String subjectToken, OAuth2AuditLog auditLog) {
        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(subjectToken);

        if (authorizationGrant == null) {
            log.trace("Failed to find authorization grant by subject_token: {}", subjectToken);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Failed to find authorization grant by subject_token."),auditLog));
        }

        final AbstractToken authorizationAccessToken = authorizationGrant.getAccessToken(subjectToken);

        if ((authorizationAccessToken == null || !authorizationAccessToken.isValid())) {
            log.error("Access token is not valid. Valid: {}", (authorizationAccessToken != null && authorizationAccessToken.isValid()));
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Access token is not valid."),auditLog));
        }

        log.trace("Invalid subject_token. Unable to validate subject_token");
        throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Invalid subject_token. Unable to validate subject_token."), auditLog));
    }

    private void validateIdToken(String subjectToken, OAuth2AuditLog auditLog) {
        try {
            final AuthorizationGrant idTokenGrant = getIdTokenGrant(subjectToken);

            if (idTokenGrant != null) { // id_token is in db
                log.debug("Found subject_token in db.");
                return;
            }

            final Jwt jwt = Jwt.parse(subjectToken);

            // verify jwt signature if we can't find it in db
            if (!cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), jwt.getHeader().getKeyId(),
                    null, null, jwt.getHeader().getSignatureAlgorithm())) {
                log.error("id_token signature verification failed.");
                throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "Invalid subject_token. id_token signature verification failed."), auditLog));
            }

            log.debug("subject_token is validated successfully as id_token.");
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

    public boolean isTxTokenFlow(HttpServletRequest httpRequest) {
        return isTxTokenFlow(httpRequest.getParameter("requested_token_type"));
    }

    public boolean isTxTokenFlow(String requestedTokenType) {
        final ExchangeTokenType exchangeTokenType = ExchangeTokenType.fromString(requestedTokenType);
        return exchangeTokenType == ExchangeTokenType.TX_TOKEN;
    }

    public Response.ResponseBuilder error(int status, TokenErrorResponseType type, String reason) {
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(errorResponseFactory.errorAsJson(type, reason));
    }

    protected AuthorizationGrant getIdTokenGrant(String idTokenHint) {
        if (org.apache.commons.lang.StringUtils.isBlank(idTokenHint)) {
            return null;
        }

        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(TokenHashUtil.hash(idTokenHint));
        if (authorizationGrant != null) {
            return authorizationGrant;
        }

        authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
        return authorizationGrant;
    }
}
