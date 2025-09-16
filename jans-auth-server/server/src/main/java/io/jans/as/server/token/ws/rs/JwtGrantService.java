package io.jans.as.server.token.ws.rs;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.common.util.CommonUtils;
import io.jans.as.model.authzdetails.AuthzDetails;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.configuration.TrustedIssuerConfig;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.authorize.ws.rs.AuthzDetailsService;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.*;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.UserService;
import io.jans.as.server.service.external.ExternalUpdateTokenService;
import io.jans.as.server.service.external.context.ExternalUpdateTokenContext;
import io.jans.as.server.util.ServerUtil;
import io.jans.util.security.StringEncrypter;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static io.jans.as.model.config.Constants.OPENID;
import static io.jans.as.model.config.Constants.TOKEN_TYPE_ACCESS_TOKEN;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Z
 */
@Stateless
@Named
public class JwtGrantService {

    public static final User EMPTY_USER = new User();

    @Inject
    private Logger log;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private ClientService clientService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private AuthzDetailsService authzDetailsService;

    @Inject
    private TokenCreatorService tokenCreatorService;

    @Inject
    private ExternalUpdateTokenService externalUpdateTokenService;

    @Inject
    private AttributeService attributeService;

    @Inject
    private UserService userService;

    public JSONObject processJwtBearer(String assertion, String scope, HttpServletRequest httpRequest, Client client, Function<JsonWebResponse, Void> idTokenPreProcessing, ExecutionContext executionContext) throws StringEncrypter.EncryptionException, CryptoProviderException {

        log.debug("processJwtBearer - started with client_id: {}, assertion: {}", client.getClientId(), assertion);

        User user = validateAssertion(assertion, client, executionContext);

        JwtBearerGrant grant = authorizationGrantList.createJwtBearerGrant(user, client);
        executionContext.setGrant(grant);

        scope = grant.checkScopesPolicy(scope);
        log.trace("Granted scopes: {}", scope);
        AuthzDetails checkedAuthzDetails = authzDetailsService.checkAuthzDetailsAndSave(executionContext.getAuthzDetails(), grant);

        AccessToken accessToken = grant.createAccessToken(executionContext); // create token after scopes are checked

        IdToken idToken = null;
        if (isTrue(appConfiguration.getOpenidScopeBackwardCompatibility()) && grant.getScopes().contains(OPENID)) {
            boolean includeIdTokenClaims = Boolean.TRUE.equals(
                    appConfiguration.getLegacyIdTokenClaims());

            ExternalUpdateTokenContext context = new ExternalUpdateTokenContext(httpRequest, grant, client, appConfiguration, attributeService);

            executionContext.setIncludeIdTokenClaims(includeIdTokenClaims);
            executionContext.setPreProcessing(idTokenPreProcessing);
            executionContext.setPostProcessor(externalUpdateTokenService.buildModifyIdTokenProcessor(context));

            idToken = grant.createIdToken(
                    null, null, null, null, null, executionContext);
        }

        RefreshToken reToken = tokenCreatorService.createRefreshToken(executionContext, scope);

        executionContext.getAuditLog().updateOAuth2AuditLog(grant, true);

        JSONObject jsonObj = new JSONObject();
        try {
            TokenRestWebServiceImpl.fillJsonObject(jsonObj, accessToken, accessToken.getTokenType(), accessToken.getExpiresIn(), reToken, scope, idToken, checkedAuthzDetails);
            jsonObj.put("issued_token_type", TOKEN_TYPE_ACCESS_TOKEN);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        log.debug("processJwtBearer - Built jsonResponse: {}", jsonObj.toString());
        
        return jsonObj;
    }

    private User validateAssertion(String assertion, Client client, ExecutionContext executionContext) throws CryptoProviderException, StringEncrypter.EncryptionException {
        log.debug("Assertion: {}", assertion);
        Jwt jwt = Jwt.parseSilently(assertion);
        if (jwt == null) {
            final String msg = "'assertion' parameter is not valid JWT.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }

        try {

            verifySignature(jwt, client, executionContext);
            verifyIssuer(jwt, executionContext);
            verifySubject(jwt, executionContext);
            verifyAudience(jwt, executionContext);
            verifyExpiration(jwt, executionContext);
            verifyNbf(jwt, executionContext);

            log.debug("Assertion validated successfully.");

            final String uid = jwt.getClaims().getClaimAsString("uid");
            if (StringUtils.isNotBlank(uid) && appConfiguration.getJwtGrantAllowUserByUidInAssertion()) {
                log.debug("Trying to find user by uid {}", uid);
                final User user = userService.getUser(uid);
                log.debug("User by uid {} {}", uid, user != null ? " is found" : " is NOT found");
                return user != null ? user : EMPTY_USER;
            }

            return EMPTY_USER;
        } catch (InvalidJwtException e) {
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, "'assertion' parameter is not valid."), executionContext.getAuditLog()));
        }
    }

    private void verifyNbf(Jwt jwt, ExecutionContext executionContext) {
        Date nbf = jwt.getClaims().getClaimAsDate(JwtClaimName.NOT_BEFORE);
        if (nbf == null || new Date().after(nbf)) {
            return;
        }

        final String msg = "Forbidden by 'nbf': " + nbf;
        log.debug(msg);
        throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
    }

    private void verifySignature(Jwt jwt, Client client, ExecutionContext executionContext) throws StringEncrypter.EncryptionException, CryptoProviderException, InvalidJwtException {
        SignatureAlgorithm signatureAlgorithm = jwt.getHeader().getSignatureAlgorithm();
        String clientSecret = clientService.decryptSecret(client.getClientSecret());

        String keyId = jwt.getHeader().getKeyId();
        JSONObject jwks = CommonUtils.getJwks(client);
        boolean validSignature = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(),
                keyId, jwks, clientSecret, signatureAlgorithm);

        if (!validSignature) {
            final String msg = "'assertion' signature is not valid.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }
    }

    private void verifyIssuer(Jwt jwt, ExecutionContext executionContext) {
        final String issuer = jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER);
        if (StringUtils.isBlank(issuer)) {
            final String msg = "'iss' claim value is absent.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }

        final Map<String, TrustedIssuerConfig> trustedIssuers = appConfiguration.getTrustedSsaIssuers();
        if (!trustedIssuers.isEmpty() && !trustedIssuers.keySet().contains(issuer)) {
            final String msg = "Issuer is not trusted. Please configure 'trustedSsaIssuers' AS configuration property.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }
    }

    private void verifySubject(Jwt jwt, ExecutionContext executionContext) {
        final String subject = jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);
        if (StringUtils.isBlank(subject)) {
            final String msg = "'sub' claim value is absent.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }
    }

    private void verifyAudience(Jwt jwt, ExecutionContext executionContext) {
        List<String> audience = jwt.getClaims().getClaimAsStringList(JwtClaimName.AUDIENCE);
        if (audience.isEmpty()) {
            final String msg = "'aud' claim value is absent.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }
        final String serverIssuer = appConfiguration.getIssuer();

        // aud must be equals to server's issuer or otherwise start with it (e.g. point to particular endpoint)
        if (audience.stream().noneMatch(aud -> aud.equals(serverIssuer) || aud.startsWith(serverIssuer))) {
            final String msg = "'aud' is invalid.";
            log.debug(msg);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
        }
    }

    public void verifyExpiration(Jwt jwt, ExecutionContext executionContext) {
        Date expirationTime = jwt.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);
        if (expirationTime != null && expirationTime.after(new Date())) {
            return;
        }

        final String msg = "'assertion' is expired";
        log.debug(msg);
        throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_REQUEST, msg), executionContext.getAuditLog()));
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
}
