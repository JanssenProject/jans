/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.userinfo.ws.rs;

import io.jans.as.common.claims.Audience;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.common.util.CommonUtils;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidClaimException;
import io.jans.as.model.exception.InvalidJweException;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwe.JweEncrypter;
import io.jans.as.model.jwe.JweEncrypterImpl;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jwk.Use;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.jwt.JwtSubClaimObject;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.model.userinfo.UserInfoErrorResponseType;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.authorize.Claim;
import io.jans.as.server.model.common.*;
import io.jans.as.server.model.userinfo.UserInfoParamsValidator;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.ScopeService;
import io.jans.as.server.service.ServerCryptoProvider;
import io.jans.as.server.service.UserService;
import io.jans.as.server.service.date.DateFormatterService;
import io.jans.as.server.service.external.ExternalDynamicScopeService;
import io.jans.as.server.service.external.context.DynamicScopeExternalContext;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.util.ServerUtil;
import io.jans.model.GluuAttribute;
import io.jans.orm.exception.EntryPersistenceException;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.*;

/**
 * Provides interface for User Info REST web services
 *
 * @author Javier Rojas Blum
 * @version October 14, 2019
 */
@Path("/")
public class UserInfoRestWebServiceImpl implements UserInfoRestWebService {

    @Inject
    private Logger log;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private ClientService clientService;

    @Inject
    private ScopeService scopeService;

    @Inject
    private AttributeService attributeService;

    @Inject
    private UserService userService;

    @Inject
    private ExternalDynamicScopeService externalDynamicScopeService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private TokenService tokenService;

    @Inject
    private DateFormatterService dateFormatterService;

    @Override
    public Response requestUserInfoGet(String accessToken, String authorization, HttpServletRequest request, SecurityContext securityContext) {
        return requestUserInfo(accessToken, authorization, request, securityContext);
    }

    @Override
    public Response requestUserInfoPost(String accessToken, String authorization, HttpServletRequest request, SecurityContext securityContext) {
        return requestUserInfo(accessToken, authorization, request, securityContext);
    }

    private Response requestUserInfo(String accessToken, String authorization, HttpServletRequest request, SecurityContext securityContext) {

        if (tokenService.isBearerAuthToken(authorization)) {
            accessToken = tokenService.getBearerToken(authorization);
        }

        log.debug("Attempting to request User Info, Access token = {}, Is Secure = {}", accessToken, securityContext.isSecure());
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.USERINFO);
        Response.ResponseBuilder builder = Response.ok();

        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(request), Action.USER_INFO);

        try {
            if (!UserInfoParamsValidator.validateParams(accessToken)) {
                return response(400, UserInfoErrorResponseType.INVALID_REQUEST, "access token is not valid.");
            }

            AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(accessToken);

            if (authorizationGrant == null) {
                log.trace("Failed to find authorization grant by access_token: {}", accessToken);
                return response(401, UserInfoErrorResponseType.INVALID_TOKEN);
            }
            oAuth2AuditLog.updateOAuth2AuditLog(authorizationGrant, false);

            final AbstractToken accessTokenObject = authorizationGrant.getAccessToken(accessToken);
            if (accessTokenObject == null || !accessTokenObject.isValid()) {
                log.trace("Invalid access token object, access_token: {}, isNull: {}, isValid: {}", accessToken, accessTokenObject == null, false);
                return response(401, UserInfoErrorResponseType.INVALID_TOKEN);
            }

            if (authorizationGrant.getAuthorizationGrantType() == AuthorizationGrantType.CLIENT_CREDENTIALS) {
                return response(403, UserInfoErrorResponseType.INSUFFICIENT_SCOPE, "Grant object has client_credentials grant_type which is not valid.");
            }
            if (appConfiguration.getOpenidScopeBackwardCompatibility()
                    && !authorizationGrant.getScopes().contains(DefaultScope.OPEN_ID.toString())
                    && !authorizationGrant.getScopes().contains(DefaultScope.PROFILE.toString())) {
                return response(403, UserInfoErrorResponseType.INSUFFICIENT_SCOPE, "Both openid and profile scopes are not present.");
            }
            if (!appConfiguration.getOpenidScopeBackwardCompatibility() && !authorizationGrant.getScopes().contains(DefaultScope.OPEN_ID.toString())) {
                return response(403, UserInfoErrorResponseType.INSUFFICIENT_SCOPE, "Missed openid scope.");
            }

            oAuth2AuditLog.updateOAuth2AuditLog(authorizationGrant, true);

            builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
            builder.header(Constants.PRAGMA, Constants.NO_CACHE);

            User currentUser = authorizationGrant.getUser();
            try {
                currentUser = userService.getUserByDn(authorizationGrant.getUserDn());
            } catch (EntryPersistenceException ex) {
                log.warn("Failed to reload user entry: '{}'", authorizationGrant.getUserDn());
            }

            if (authorizationGrant.getClient() != null
                    && authorizationGrant.getClient().getUserInfoEncryptedResponseAlg() != null
                    && authorizationGrant.getClient().getUserInfoEncryptedResponseEnc() != null) {
                KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm.fromName(authorizationGrant.getClient().getUserInfoEncryptedResponseAlg());
                BlockEncryptionAlgorithm blockEncryptionAlgorithm = BlockEncryptionAlgorithm.fromName(authorizationGrant.getClient().getUserInfoEncryptedResponseEnc());
                builder.type("application/jwt");
                builder.entity(getJweResponse(
                        keyEncryptionAlgorithm,
                        blockEncryptionAlgorithm,
                        currentUser,
                        authorizationGrant,
                        authorizationGrant.getScopes()));
            } else if (authorizationGrant.getClient() != null
                    && authorizationGrant.getClient().getUserInfoSignedResponseAlg() != null) {
                SignatureAlgorithm algorithm = SignatureAlgorithm.fromString(authorizationGrant.getClient().getUserInfoSignedResponseAlg());
                builder.type("application/jwt");
                builder.entity(getJwtResponse(algorithm,
                        currentUser,
                        authorizationGrant,
                        authorizationGrant.getScopes()));
            } else {
                builder.type((MediaType.APPLICATION_JSON + ";charset=UTF-8"));
                builder.entity(getJSonResponse(currentUser,
                        authorizationGrant,
                        authorizationGrant.getScopes()));
            }
            return builder.build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).build(); // 500
        } finally {
            applicationAuditLogger.sendMessage(oAuth2AuditLog);
        }
    }

    private Response response(int status, UserInfoErrorResponseType errorResponseType) {
        return response(status, errorResponseType, "");
    }

    private Response response(int status, UserInfoErrorResponseType errorResponseType, String reason) {
        return Response
                .status(status)
                .entity(errorResponseFactory.errorAsJson(errorResponseType, reason))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate())
                .build();
    }

    private String getJwtResponse(SignatureAlgorithm signatureAlgorithm, User user, AuthorizationGrant authorizationGrant,
                                  Collection<String> scopes) throws Exception {
        log.trace("Building JWT reponse with next scopes {0} for user {1} and user custom attributes {0}", scopes, user.getUserId(), user.getCustomAttributes());

        Jwt jwt = new Jwt();

        // Header
        jwt.getHeader().setType(JwtType.JWT);
        jwt.getHeader().setAlgorithm(signatureAlgorithm);

        String keyId = new ServerCryptoProvider(cryptoProvider).getKeyId(webKeysConfiguration, Algorithm.fromString(signatureAlgorithm.getName()), Use.SIGNATURE);
        if (keyId != null) {
            jwt.getHeader().setKeyId(keyId);
        }

        // Claims
        jwt.setClaims(createJwtClaims(user, authorizationGrant, scopes));

        // Signature
        String sharedSecret = clientService.decryptSecret(authorizationGrant.getClient().getClientSecret());
        String signature = cryptoProvider.sign(jwt.getSigningInput(), jwt.getHeader().getKeyId(), sharedSecret, signatureAlgorithm);
        jwt.setEncodedSignature(signature);

        return jwt.toString();
    }

    private JwtClaims createJwtClaims(User user, AuthorizationGrant authorizationGrant, Collection<String> scopes) throws ParseException, InvalidClaimException {
        String claimsString = getJSonResponse(user, authorizationGrant, scopes);
        JwtClaims claims = new JwtClaims(new JSONObject(claimsString));

        claims.setIssuer(appConfiguration.getIssuer());
        Audience.setAudience(claims, authorizationGrant.getClient());
        return claims;
    }

    public String getJweResponse(
            KeyEncryptionAlgorithm keyEncryptionAlgorithm, BlockEncryptionAlgorithm blockEncryptionAlgorithm,
            User user, AuthorizationGrant authorizationGrant, Collection<String> scopes) throws Exception {
        log.trace("Building JWE reponse with next scopes {0} for user {1} and user custom attributes {0}", scopes, user.getUserId(), user.getCustomAttributes());

        Jwe jwe = new Jwe();

        // Header
        jwe.getHeader().setType(JwtType.JWT);
        jwe.getHeader().setAlgorithm(keyEncryptionAlgorithm);
        jwe.getHeader().setEncryptionMethod(blockEncryptionAlgorithm);

        // Claims
        jwe.setClaims(createJwtClaims(user, authorizationGrant, scopes));

        // Encryption
        if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA_OAEP
                || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA1_5) {
            JSONObject jsonWebKeys = CommonUtils.getJwks(authorizationGrant.getClient());
            String keyId = new ServerCryptoProvider(cryptoProvider).getKeyId(JSONWebKeySet.fromJSONObject(jsonWebKeys),
                    Algorithm.fromString(keyEncryptionAlgorithm.getName()),
                    Use.ENCRYPTION);
            PublicKey publicKey = cryptoProvider.getPublicKey(keyId, jsonWebKeys, null);

            if (publicKey != null) {
                JweEncrypter jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, publicKey);
                jwe = jweEncrypter.encrypt(jwe);
            } else {
                throw new InvalidJweException("The public key is not valid");
            }
        } else if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A128KW
                || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A256KW) {
            try {
                byte[] sharedSymmetricKey = clientService.decryptSecret(authorizationGrant.getClient().getClientSecret()).getBytes(StandardCharsets.UTF_8);
                JweEncrypter jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, sharedSymmetricKey);
                jwe = jweEncrypter.encrypt(jwe);
            } catch (Exception e) {
                throw new InvalidJweException(e);
            }
        }

        return jwe.toString();
    }

    /**
     * Builds a JSon String with the response parameters.
     */
    public String getJSonResponse(User user, AuthorizationGrant authorizationGrant, Collection<String> scopes) throws InvalidClaimException, ParseException {
        log.trace("Building JSON reponse with next scopes {} for user {} and user custom attributes {}", scopes, user.getUserId(), user.getCustomAttributes());

        JsonWebResponse jsonWebResponse = new JsonWebResponse();

        // Claims
        List<Scope> dynamicScopes = new ArrayList<>();
        for (String scopeName : scopes) {
            Scope scope = scopeService.getScopeById(scopeName);
            if ((scope != null) && (ScopeType.DYNAMIC == scope.getScopeType())) {
                dynamicScopes.add(scope);
                continue;
            }

            Map<String, Object> claims = scopeService.getClaims(user, scope);
            if (claims == null) {
                continue;
            }
            if (scope == null) {
                log.trace("Unable to find scope in persistence. Is it removed? Scope name: {}", scopeName);
            }

            if (scope != null && Boolean.TRUE.equals(scope.isGroupClaims())) {
                JwtSubClaimObject groupClaim = new JwtSubClaimObject();
                groupClaim.setName(scope.getId());
                for (Map.Entry<String, Object> entry : claims.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (value instanceof List) {
                        groupClaim.setClaim(key, (List<String>) value);
                    } else {
                        groupClaim.setClaim(key, String.valueOf(value));
                    }
                }

                jsonWebResponse.getClaims().setClaim(scope.getId(), groupClaim);
            } else {
                for (Map.Entry<String, Object> entry : claims.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (value instanceof List) {
                        jsonWebResponse.getClaims().setClaim(key, (List<String>) value);
                    } else if (value instanceof Boolean) {
                        jsonWebResponse.getClaims().setClaim(key, (Boolean) value);
                    } else if (value instanceof Date) {
                        Serializable formattedValue = dateFormatterService.formatClaim((Date) value, key);
                        jsonWebResponse.getClaims().setClaimObject(key, formattedValue, true);
                    } else {
                        jsonWebResponse.getClaims().setClaim(key, String.valueOf(value));
                    }
                }
            }
        }

        if (authorizationGrant.getClaims() != null) {
            JSONObject claimsObj = new JSONObject(authorizationGrant.getClaims());
            if (claimsObj.has("userinfo")) {
                JSONObject userInfoObj = claimsObj.getJSONObject("userinfo");
                for (Iterator<String> it = userInfoObj.keys(); it.hasNext(); ) {
                    String claimName = it.next();
                    boolean optional = true; // ClaimValueType.OPTIONAL.equals(claim.getClaimValue().getClaimValueType());
                    GluuAttribute gluuAttribute = attributeService.getByClaimName(claimName);

                    if (gluuAttribute != null) {
                        String ldapClaimName = gluuAttribute.getName();

                        Object attribute = user.getAttribute(ldapClaimName, optional, gluuAttribute.getOxMultiValuedAttribute());
                        jsonWebResponse.getClaims().setClaimFromJsonObject(claimName, attribute);
                    }
                }
            }
        }

        if (authorizationGrant.getJwtAuthorizationRequest() != null
                && authorizationGrant.getJwtAuthorizationRequest().getUserInfoMember() != null) {
            for (Claim claim : authorizationGrant.getJwtAuthorizationRequest().getUserInfoMember().getClaims()) {
                boolean optional = true; // ClaimValueType.OPTIONAL.equals(claim.getClaimValue().getClaimValueType());
                GluuAttribute gluuAttribute = attributeService.getByClaimName(claim.getName());

                if (gluuAttribute != null) {
                    Client client = authorizationGrant.getClient();

                    if (validateRequesteClaim(gluuAttribute, client.getClaims(), scopes)) {
                        String ldapClaimName = gluuAttribute.getName();
                        Object attribute = user.getAttribute(ldapClaimName, optional, gluuAttribute.getOxMultiValuedAttribute());
                        jsonWebResponse.getClaims().setClaimFromJsonObject(claim.getName(), attribute);
                    }
                }
            }
        }

        jsonWebResponse.getClaims().setSubjectIdentifier(authorizationGrant.getSub());

        if ((dynamicScopes.size() > 0) && externalDynamicScopeService.isEnabled()) {
            final UnmodifiableAuthorizationGrant unmodifiableAuthorizationGrant = new UnmodifiableAuthorizationGrant(authorizationGrant);
            DynamicScopeExternalContext dynamicScopeContext = new DynamicScopeExternalContext(dynamicScopes, jsonWebResponse, unmodifiableAuthorizationGrant);
            externalDynamicScopeService.executeExternalUpdateMethods(dynamicScopeContext);
        }

        return jsonWebResponse.toString();
    }

    public boolean validateRequesteClaim(GluuAttribute gluuAttribute, String[] clientAllowedClaims, Collection<String> scopes) {
        if (gluuAttribute == null) {
            log.trace("gluuAttribute is null.");
            return false;
        }
        if (clientAllowedClaims != null) {
            for (String clientAllowedClaim : clientAllowedClaims) {
                if (gluuAttribute.getDn().equals(clientAllowedClaim)) {
                    return true;
                }
            }
        }

        for (String scopeName : scopes) {
            Scope scope = scopeService.getScopeById(scopeName);

            if (scope != null && scope.getClaims() != null) {
                for (String claimDn : scope.getClaims()) {
                    if (gluuAttribute.getDisplayName().equals(attributeService.getAttributeByDn(claimDn).getDisplayName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}