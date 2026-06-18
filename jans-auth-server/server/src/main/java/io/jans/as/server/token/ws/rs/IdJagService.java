package io.jans.as.server.token.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.configuration.TrustedIssuerConfig;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.token.TokenErrorResponseType;
import org.json.JSONArray;
import org.json.JSONObject;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.model.token.JwtSigner;
import io.jans.as.server.service.external.ExternalIdentityAssertionService;
import io.jans.as.server.util.ServerUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.jans.as.model.config.Constants.*;

/**
 * Issues Identity Assertion JWT Authorization Grants (ID-JAGs) when jans-auth-server
 * acts as IdP Authorization Server in the Cross-App Access (XAA) flow.
 *
 * Spec: draft-ietf-oauth-identity-assertion-authz-grant-04
 *
 * @author Yuriy Z
 */
@Stateless
@Named
public class IdJagService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private ExternalIdentityAssertionService externalIdentityAssertionService;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    /**
     * Issues a signed ID-JAG for the given execution context.
     */
    public String issueIdJag(ExecutionContext executionContext, Jwt subjectJwt, String audience, String scope) {
        return issueIdJag(executionContext, subjectJwt, audience, scope, null, null);
    }

    /**
     * Issues a signed ID-JAG for the given execution context.
     *
     * @param executionContext    context containing client, http request, audit log
     * @param subjectJwt         validated subject token JWT (ID token or similar)
     * @param audience           Resource Authorization Server issuer URI (target audience)
     * @param scope              optional scope string to embed in the ID-JAG
     * @param resource           optional resource indicator (RFC 8707) to embed in the ID-JAG
     * @param authorizationDetails optional authorization_details JSON array string to embed in the ID-JAG
     * @return signed ID-JAG as JWT string
     */
    public String issueIdJag(ExecutionContext executionContext, Jwt subjectJwt, String audience, String scope,
                             String resource, String authorizationDetails) {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.IDENTITY_ASSERTION_AUTHZ_GRANT);

        final Client client = executionContext.getClient();
        final OAuth2AuditLog auditLog = executionContext.getAuditLog();

        if (StringUtils.isBlank(audience)) {
            final String msg = "'audience' parameter is required for ID-JAG token exchange.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), auditLog));
        }

        try {
            final JwtSigner signer = newJwtSigner(audience);
            final Jwt idJag = signer.newJwt();
            idJag.getHeader().setType(JwtType.OAUTH_ID_JAG);
            populateIdJagClaims(idJag, client, subjectJwt, audience, scope, resource, authorizationDetails);
            applyModifyIdJagPayloadScript(idJag, executionContext);
            final Jwt signed = signer.sign();
            log.debug("Issued ID-JAG for client: {}, audience: {}", client.getClientId(), audience);
            return signed.toString();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to issue ID-JAG", e);
            throw new WebApplicationException(response(error(500, TokenErrorResponseType.INVALID_GRANT, "Failed to issue ID-JAG."), auditLog));
        }
    }

    private void populateIdJagClaims(Jwt idJag, Client client, Jwt subjectJwt, String audience, String scope,
                                     String resource, String authorizationDetails) {
        final Calendar cal = Calendar.getInstance();
        final Date issuedAt = cal.getTime();
        cal.add(Calendar.SECOND, appConfiguration.getIdJagLifetime());
        final Date expiration = cal.getTime();

        // iss is already set by JwtSigner.newJwt(); override aud with requested audience
        idJag.getClaims().setAudience(audience);
        idJag.getClaims().setSubjectIdentifier(extractSub(subjectJwt));
        idJag.getClaims().setClaim(JwtClaimName.CLIENT_ID, client.getClientId());
        idJag.getClaims().setClaim(JwtClaimName.JWT_ID, UUID.randomUUID().toString());
        idJag.getClaims().setExpirationTime(expiration);
        idJag.getClaims().setIat(issuedAt);

        // §4.3.3: MUST include granted scope, resource, authorization_details if present
        if (StringUtils.isNotBlank(scope)) {
            idJag.getClaims().setClaim(JwtClaimName.SCOPE, scope);
        }
        if (StringUtils.isNotBlank(resource)) {
            idJag.getClaims().setClaim(JwtClaimName.RESOURCE, resource);
        }
        if (StringUtils.isNotBlank(authorizationDetails)) {
            idJag.getClaims().setClaim(JwtClaimName.AUTHORIZATION_DETAILS, new JSONArray(authorizationDetails));
        }

        propagateOptionalClaims(idJag, subjectJwt);
    }

    private String extractSub(Jwt subjectJwt) {
        if (subjectJwt == null) {
            return "";
        }
        final String sub = subjectJwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);
        return StringUtils.defaultString(sub);
    }

    private void propagateOptionalClaims(Jwt idJag, Jwt subjectJwt) {
        if (subjectJwt == null) {
            return;
        }
        copyClaimIfPresent(idJag, subjectJwt, JwtClaimName.EMAIL);
        copyClaimIfPresent(idJag, subjectJwt, JwtClaimName.AUTHENTICATION_TIME);
        copyClaimIfPresent(idJag, subjectJwt, JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE);
        copyClaimIfPresent(idJag, subjectJwt, JwtClaimName.AUTHENTICATION_METHOD_REFERENCES);
    }

    private void copyClaimIfPresent(Jwt dest, Jwt src, String claimName) {
        final Object value = src.getClaims().getClaim(claimName);
        if (value != null) {
            dest.getClaims().setClaimObject(claimName, value, true);
        }
    }

    private void applyModifyIdJagPayloadScript(Jwt idJag, ExecutionContext executionContext) {
        try {
            final JSONObject claimsSnapshot = idJag.getClaims().toJsonObject();
            if (externalIdentityAssertionService.externalModifyIdJagPayload(idJag, executionContext)) {
                log.debug("Successfully ran identity-assertion script modifyIdJagPayload.");
            } else {
                idJag.getClaims().load(claimsSnapshot);
                log.trace("Reverted ID-JAG claims: identity-assertion script modifyIdJagPayload returned false.");
            }
        } catch (InvalidJwtException e) {
            log.error("Failed to snapshot ID-JAG claims for script revert", e);
        }
    }

    private JwtSigner newJwtSigner(String audience) {
        final SignatureAlgorithm alg = SignatureAlgorithm.fromString(appConfiguration.getDefaultSignatureAlgorithm());
        return new JwtSigner(appConfiguration, webKeysConfiguration, alg, audience);
    }

    /**
     * Validates the incoming subject token for an ID-JAG token exchange request.
     * Returns the parsed subject JWT on success; throws WebApplicationException on failure.
     */
    public Jwt validateSubjectToken(String subjectToken, String subjectTokenType, ExecutionContext executionContext) {
        if (StringUtils.isBlank(subjectToken)) {
            final String msg = "'subject_token' is required.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }

        if (SUBJECT_TOKEN_TYPE_SAML2.equalsIgnoreCase(subjectTokenType)) {
            // SAML2 subject_token accepted as-is; subject resolution done by caller
            log.debug("subject_token_type is saml2; accepting opaque subject_token for ID-JAG issuance.");
            return null;
        }

        // §4.3.3: If subject_token is a Refresh Token, IdP MUST validate it as a standard refresh_token grant
        if (SUBJECT_TOKEN_TYPE_REFRESH_TOKEN.equalsIgnoreCase(subjectTokenType)) {
            return validateRefreshTokenSubject(subjectToken, executionContext);
        }

        final Jwt jwt = Jwt.parseSilently(subjectToken);
        if (jwt == null) {
            final String msg = "'subject_token' is not a valid JWT.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }

        verifySubjectTokenSignature(jwt, executionContext);
        verifySubjectTokenIssuer(jwt, executionContext);
        verifySubjectTokenAudience(jwt, executionContext.getClient(), executionContext);
        verifySubjectTokenExpiry(jwt, executionContext);
        return jwt;
    }

    private Jwt validateRefreshTokenSubject(String subjectToken, ExecutionContext executionContext) {
        final String clientId = executionContext.getClient().getClientId();
        final AuthorizationGrant grant = authorizationGrantList.getAuthorizationGrantByRefreshToken(clientId, subjectToken);
        if (grant == null) {
            final String msg = "subject_token refresh_token is invalid or expired.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, msg), executionContext.getAuditLog()));
        }
        log.debug("subject_token refresh_token is valid for client: {}", clientId);
        return null;
    }

    private void verifySubjectTokenSignature(Jwt jwt, ExecutionContext executionContext) {
        try {
            final boolean valid = cryptoProvider.verifySignature(
                    jwt.getSigningInput(),
                    jwt.getEncodedSignature(),
                    jwt.getHeader().getKeyId(),
                    null,
                    null,
                    jwt.getHeader().getSignatureAlgorithm());
            if (!valid) {
                final String msg = "subject_token signature verification failed.";
                log.debug(msg);
                throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("subject_token signature verification error", e);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, "subject_token signature verification failed."), executionContext.getAuditLog()));
        }
    }

    private void verifySubjectTokenIssuer(Jwt jwt, ExecutionContext executionContext) {
        final String issuer = jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER);
        if (StringUtils.isBlank(issuer)) {
            final String msg = "subject_token 'iss' claim is absent.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }
        final Map<String, TrustedIssuerConfig> trustedIssuers = appConfiguration.getIdJagTrustedIdpIssuers();
        final String serverIssuer = appConfiguration.getIssuer();
        if (!trustedIssuers.isEmpty() && !trustedIssuers.containsKey(issuer) && !issuer.equals(serverIssuer)) {
            final String msg = "subject_token issuer '" + issuer + "' is not in idJagTrustedIdpIssuers.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, msg), executionContext.getAuditLog()));
        }
    }

    private void verifySubjectTokenAudience(Jwt jwt, Client client, ExecutionContext executionContext) {
        final List<String> audience = jwt.getClaims().getClaimAsStringList(JwtClaimName.AUDIENCE);
        if (audience.isEmpty()) {
            final String msg = "subject_token 'aud' claim is absent.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }
        final String clientId = client.getClientId();
        if (audience.stream().noneMatch(aud -> aud.equals(clientId))) {
            final String msg = "subject_token 'aud' does not contain the requesting client_id.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, msg), executionContext.getAuditLog()));
        }
    }

    private void verifySubjectTokenExpiry(Jwt jwt, ExecutionContext executionContext) {
        final Date exp = jwt.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);
        if (exp == null || exp.before(new Date())) {
            final String msg = exp == null ? "subject_token 'exp' claim is absent." : "subject_token is expired.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }
    }

    /**
     * Builds the token exchange response JSON containing the issued ID-JAG.
     */
    public JSONObject buildTokenExchangeResponse(String idJagJwt) {
        final JSONObject json = new JSONObject();
        json.put("access_token", idJagJwt);
        json.put("issued_token_type", TOKEN_TYPE_ID_JAG);
        json.put("token_type", "N_A");
        return json;
    }

    private Response response(Response.ResponseBuilder builder, OAuth2AuditLog auditLog) {
        builder.cacheControl(ServerUtil.cacheControl(true, false));
        builder.header("Pragma", "no-cache");
        applicationAuditLogger.sendMessage(auditLog);
        return builder.build();
    }

    public Response.ResponseBuilder error(int status, TokenErrorResponseType type, String reason) {
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE)
                .entity(errorResponseFactory.errorAsJson(type, reason));
    }
}
