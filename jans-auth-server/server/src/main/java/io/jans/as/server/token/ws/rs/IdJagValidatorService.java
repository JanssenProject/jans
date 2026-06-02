package io.jans.as.server.token.ws.rs;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.configuration.TrustedIssuerConfig;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.UserService;
import io.jans.as.server.util.ServerUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Validates Identity Assertion JWT Authorization Grants (ID-JAGs) when
 * jans-auth-server acts as Resource Authorization Server in the XAA flow.
 *
 * Spec: draft-ietf-oauth-identity-assertion-authz-grant-04
 *
 * @author Yuriy Z
 */
@Stateless
@Named
public class IdJagValidatorService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private UserService userService;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    /**
     * Returns true when the JWT is an ID-JAG (typ header = "oauth-id-jag+jwt").
     */
    public boolean isIdJag(Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        return JwtType.OAUTH_ID_JAG == jwt.getHeader().getType();
    }

    /**
     * Validates an ID-JAG assertion and resolves the local user.
     * Throws WebApplicationException on any validation failure.
     *
     * @param jwt              the parsed ID-JAG
     * @param client           the authenticated OAuth client
     * @param executionContext current execution context
     * @return the resolved local User, or EMPTY_USER if subject cannot be mapped
     */
    public User validateIdJag(Jwt jwt, Client client, ExecutionContext executionContext) {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.IDENTITY_ASSERTION_AUTHZ_GRANT);

        log.debug("Validating ID-JAG for client: {}", client.getClientId());

        try {
            verifyTypHeader(jwt, executionContext);
            verifySignature(jwt, executionContext);
            verifyIssuer(jwt, executionContext);
            verifyAudience(jwt, executionContext);
            verifyClientId(jwt, client, executionContext);
            verifyExpiration(jwt, executionContext);
            verifyAuthorizationDetailsIfPresent(jwt, executionContext);

            final User user = resolveSubject(jwt, executionContext);
            log.debug("ID-JAG validated successfully for client: {}", client.getClientId());
            return user;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("ID-JAG validation failed", e);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, "ID-JAG validation failed."), executionContext.getAuditLog()));
        }
    }

    private void verifyTypHeader(Jwt jwt, ExecutionContext executionContext) {
        if (!isIdJag(jwt)) {
            final String msg = "ID-JAG 'typ' header must be 'oauth-id-jag+jwt'.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }
    }

    private void verifySignature(Jwt jwt, ExecutionContext executionContext) throws CryptoProviderException, InvalidJwtException {
        // Verify signature using AS's own crypto provider (for same-instance deployments)
        // or using keys from a configured JWKS URI for external IdP AS.
        // For now verify with server's local keys (cross-instance JWKS fetching is a future extension).
        final boolean validSignature = cryptoProvider.verifySignature(
                jwt.getSigningInput(),
                jwt.getEncodedSignature(),
                jwt.getHeader().getKeyId(),
                null,
                null,
                jwt.getHeader().getSignatureAlgorithm());

        if (!validSignature) {
            final String msg = "ID-JAG signature verification failed.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }
    }

    private void verifyIssuer(Jwt jwt, ExecutionContext executionContext) {
        final String issuer = jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER);
        if (StringUtils.isBlank(issuer)) {
            final String msg = "ID-JAG 'iss' claim is absent.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }

        final Map<String, TrustedIssuerConfig> trustedIssuers = appConfiguration.getIdJagTrustedIdpIssuers();
        if (!trustedIssuers.isEmpty() && !trustedIssuers.containsKey(issuer)) {
            final String msg = "ID-JAG issuer '" + issuer + "' is not in idJagTrustedIdpIssuers.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, msg), executionContext.getAuditLog()));
        }
    }

    private void verifyAudience(Jwt jwt, ExecutionContext executionContext) {
        final List<String> audience = jwt.getClaims().getClaimAsStringList(JwtClaimName.AUDIENCE);
        if (audience.isEmpty()) {
            final String msg = "ID-JAG 'aud' claim is absent.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }

        final String serverIssuer = appConfiguration.getIssuer();
        if (audience.stream().noneMatch(aud -> aud.equals(serverIssuer) || aud.startsWith(serverIssuer))) {
            final String msg = "ID-JAG 'aud' does not match this AS issuer. Expected: " + serverIssuer;
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, msg), executionContext.getAuditLog()));
        }
    }

    private void verifyClientId(Jwt jwt, Client client, ExecutionContext executionContext) {
        final String idJagClientId = jwt.getClaims().getClaimAsString(JwtClaimName.CLIENT_ID);
        if (StringUtils.isBlank(idJagClientId)) {
            final String msg = "ID-JAG 'client_id' claim is absent.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }

        if (!idJagClientId.equals(client.getClientId())) {
            final String msg = "ID-JAG 'client_id' does not match the authenticated client.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, msg), executionContext.getAuditLog()));
        }
    }

    private void verifyAuthorizationDetailsIfPresent(Jwt jwt, ExecutionContext executionContext) {
        // §4.4.1: If authorization_details is present, the Resource AS MUST parse it as a JSON array
        final Object authzDetails = jwt.getClaims().getClaim(JwtClaimName.AUTHORIZATION_DETAILS);
        if (authzDetails == null) {
            return;
        }
        if (authzDetails instanceof JSONArray) {
            return;
        }
        try {
            new JSONArray(String.valueOf(authzDetails));
        } catch (JSONException e) {
            final String msg = "ID-JAG 'authorization_details' claim is not a valid JSON array.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }
    }

    private void verifyExpiration(Jwt jwt, ExecutionContext executionContext) {
        final Date exp = jwt.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);
        if (exp == null || exp.before(new Date())) {
            final String msg = "ID-JAG is expired or 'exp' claim is absent.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, msg), executionContext.getAuditLog()));
        }
    }

    /**
     * Resolves the local User from ID-JAG subject claims.
     * Resolution order: sub → email → aud_sub → EMPTY_USER.
     */
    private User resolveSubject(Jwt jwt, ExecutionContext executionContext) {
        // Try sub first
        final String sub = jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);
        if (StringUtils.isNotBlank(sub)) {
            final User user = userService.getUser(sub);
            if (user != null) {
                log.debug("Resolved user by 'sub': {}", sub);
                return user;
            }
        }

        // Fallback: email
        final String email = jwt.getClaims().getClaimAsString(JwtClaimName.EMAIL);
        if (StringUtils.isNotBlank(email)) {
            final User user = userService.getUserByAttribute("mail", email);
            if (user != null) {
                log.debug("Resolved user by 'email': {}", email);
                return user;
            }
        }

        // Fallback: aud_sub (Resource AS-specific subject identifier)
        final String audSub = jwt.getClaims().getClaimAsString(JwtClaimName.AUD_SUB);
        if (StringUtils.isNotBlank(audSub)) {
            final User user = userService.getUser(audSub);
            if (user != null) {
                log.debug("Resolved user by 'aud_sub': {}", audSub);
                return user;
            }
        }

        log.debug("Could not resolve local user from ID-JAG; using empty user.");
        return JwtGrantService.EMPTY_USER;
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
