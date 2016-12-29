/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.userinfo.ws.rs;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.audit.ApplicationAuditLogger;
import org.xdi.oxauth.model.audit.Action;
import org.xdi.oxauth.model.audit.OAuth2AuditLog;
import org.xdi.oxauth.model.authorize.Claim;
import org.xdi.oxauth.model.common.*;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.crypto.AbstractCryptoProvider;
import org.xdi.oxauth.model.crypto.CryptoProviderFactory;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.exception.InvalidClaimException;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwe.Jwe;
import org.xdi.oxauth.model.jwe.JweEncrypter;
import org.xdi.oxauth.model.jwe.JweEncrypterImpl;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtSubClaimObject;
import org.xdi.oxauth.model.jwt.JwtType;
import org.xdi.oxauth.model.ldap.PairwiseIdentifier;
import org.xdi.oxauth.model.token.JsonWebResponse;
import org.xdi.oxauth.model.userinfo.UserInfoErrorResponseType;
import org.xdi.oxauth.model.userinfo.UserInfoParamsValidator;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.PairwiseIdentifierService;
import org.xdi.oxauth.service.ScopeService;
import org.xdi.oxauth.service.UserService;
import org.xdi.oxauth.service.external.ExternalDynamicScopeService;
import org.xdi.oxauth.service.external.context.DynamicScopeExternalContext;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.security.StringEncrypter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.UnsupportedEncodingException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.*;

/**
 * Provides interface for User Info REST web services
 *
 * @author Javier Rojas Blum
 * @version August 17, 2016
 */
@Name("requestUserInfoRestWebService")
public class UserInfoRestWebServiceImpl implements UserInfoRestWebService {

    @Logger
    private Log log;

    @In
    private ApplicationAuditLogger applicationAuditLogger;

    @In
    private ErrorResponseFactory errorResponseFactory;

    @In
    private AuthorizationGrantList authorizationGrantList;

    @In
    private ScopeService scopeService;

    @In
    private AttributeService attributeService;

    @In
    private UserService userService;

    @In
    private ExternalDynamicScopeService externalDynamicScopeService;

    @In
    private PairwiseIdentifierService pairwiseIdentifierService;

    @In
    private AppConfiguration appConfiguration;

    @In
    private JSONWebKeySet webKeysConfiguration;

    @Override
    public Response requestUserInfoGet(String accessToken, String authorization, HttpServletRequest request, SecurityContext securityContext) {
        return requestUserInfo(accessToken, authorization, request, securityContext);
    }

    @Override
    public Response requestUserInfoPost(String accessToken, String authorization, HttpServletRequest request, SecurityContext securityContext) {
        return requestUserInfo(accessToken, authorization, request, securityContext);
    }

    public Response requestUserInfo(String accessToken, String authorization, HttpServletRequest request, SecurityContext securityContext) {
        if (authorization != null && !authorization.isEmpty() && authorization.startsWith("Bearer ")) {
            accessToken = authorization.substring(7);
        }
        log.debug("Attempting to request User Info, Access token = {0}, Is Secure = {1}",
                accessToken, securityContext.isSecure());
        Response.ResponseBuilder builder = Response.ok();

        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(request), Action.USER_INFO);

        try {
            if (!UserInfoParamsValidator.validateParams(accessToken)) {
                builder = Response.status(400);
                builder.entity(errorResponseFactory.getErrorAsJson(UserInfoErrorResponseType.INVALID_REQUEST));
            } else {
                AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(accessToken);

                if (authorizationGrant == null) {
                    builder = Response.status(400);
                    builder.entity(errorResponseFactory.getErrorAsJson(UserInfoErrorResponseType.INVALID_TOKEN));
                } else if (!authorizationGrant.getScopes().contains(DefaultScope.OPEN_ID.toString())
                        && !authorizationGrant.getScopes().contains(DefaultScope.PROFILE.toString())) {
                    builder = Response.status(403);
                    builder.entity(errorResponseFactory.getErrorAsJson(UserInfoErrorResponseType.INSUFFICIENT_SCOPE));
                    oAuth2AuditLog.updateOAuth2AuditLog(authorizationGrant, false);
                } else {
                    oAuth2AuditLog.updateOAuth2AuditLog(authorizationGrant, true);
                    CacheControl cacheControl = new CacheControl();
                    cacheControl.setPrivate(true);
                    cacheControl.setNoTransform(false);
                    cacheControl.setNoStore(true);
                    builder.cacheControl(cacheControl);
                    builder.header("Pragma", "no-cache");

                    User currentUser = authorizationGrant.getUser();
                    try {
                        currentUser = userService.getUserByDn(authorizationGrant.getUserDn());
                    } catch (EntryPersistenceException ex) {
                        log.warn("Failed to reload user entry: '{0}'", authorizationGrant.getUserDn());
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
                }
            }
        } catch (StringEncrypter.EncryptionException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()); // 500
            log.error(e.getMessage(), e);
        } catch (InvalidJwtException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()); // 500
            log.error(e.getMessage(), e);
        } catch (SignatureException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()); // 500
            log.error(e.getMessage(), e);
        } catch (InvalidClaimException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()); // 500
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()); // 500
            log.error(e.getMessage(), e);
        }

        applicationAuditLogger.sendMessage(oAuth2AuditLog);
        return builder.build();
    }

    public String getJwtResponse(SignatureAlgorithm signatureAlgorithm, User user, AuthorizationGrant authorizationGrant,
                                 Collection<String> scopes) throws Exception {
        Jwt jwt = new Jwt();
        AbstractCryptoProvider cryptoProvider = CryptoProviderFactory.getCryptoProvider(appConfiguration);

        // Header
        jwt.getHeader().setType(JwtType.JWT);
        jwt.getHeader().setAlgorithm(signatureAlgorithm);

        String keyId = cryptoProvider.getKeyId(webKeysConfiguration, signatureAlgorithm);
        if (keyId != null) {
            jwt.getHeader().setKeyId(keyId);
        }

        // Claims
        List<String> dynamicScopes = new ArrayList<String>();
        for (String scopeName : scopes) {
            Scope scope = scopeService.getScopeByDisplayName(scopeName);
            if (org.xdi.oxauth.model.common.ScopeType.DYNAMIC == scope.getScopeType()) {
                dynamicScopes.add(scope.getDisplayName());
                continue;
            }

            if (scope.getOxAuthClaims() != null) {
                for (String claimDn : scope.getOxAuthClaims()) {
                    GluuAttribute gluuAttribute = attributeService.getAttributeByDn(claimDn);

                    String claimName = gluuAttribute.getOxAuthClaimName();
                    String ldapName = gluuAttribute.getName();
                    String attributeValue = null;

                    if (StringUtils.isNotBlank(claimName) && StringUtils.isNotBlank(ldapName)) {
                        if (ldapName.equals("uid")) {
                            attributeValue = user.getUserId();
                        } else {
                            attributeValue = user.getAttribute(gluuAttribute.getName());
                        }

                        jwt.getClaims().setClaim(claimName, attributeValue);
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
                    String ldapClaimName = gluuAttribute.getName();
                    Object attribute = user.getAttribute(ldapClaimName, optional);
                    if (attribute != null) {
                        if (attribute instanceof JSONArray) {
                            JSONArray jsonArray = (JSONArray) attribute;
                            List<String> values = new ArrayList<String>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                String value = jsonArray.optString(i);
                                if (value != null) {
                                    values.add(value);
                                }
                            }
                            jwt.getClaims().setClaim(claim.getName(), values);
                        } else {
                            String value = (String) attribute;
                            jwt.getClaims().setClaim(claim.getName(), value);
                        }
                    }
                }
            }
        }

        // Check for Subject Identifier Type
        if (authorizationGrant.getClient().getSubjectType() != null &&
                SubjectType.fromString(authorizationGrant.getClient().getSubjectType()).equals(SubjectType.PAIRWISE)) {
            String sectorIdentifierUri = null;
            if (StringUtils.isNotBlank(authorizationGrant.getClient().getSectorIdentifierUri())) {
                sectorIdentifierUri = authorizationGrant.getClient().getSectorIdentifierUri();
            } else {
                sectorIdentifierUri = authorizationGrant.getClient().getRedirectUris()[0];
            }

            String userInum = authorizationGrant.getUser().getAttribute("inum");
            PairwiseIdentifier pairwiseIdentifier = pairwiseIdentifierService.findPairWiseIdentifier(
                    userInum, sectorIdentifierUri);
            if (pairwiseIdentifier == null) {
                pairwiseIdentifier = new PairwiseIdentifier(sectorIdentifierUri);
                pairwiseIdentifier.setId(UUID.randomUUID().toString());
                pairwiseIdentifier.setDn(pairwiseIdentifierService.getDnForPairwiseIdentifier(
                        pairwiseIdentifier.getId(),
                        userInum));
                pairwiseIdentifierService.addPairwiseIdentifier(userInum, pairwiseIdentifier);
            }
            jwt.getClaims().setSubjectIdentifier(pairwiseIdentifier.getId());
        } else {
            String openidSubAttribute = appConfiguration.getOpenidSubAttribute();
            jwt.getClaims().setSubjectIdentifier(authorizationGrant.getUser().getAttribute(openidSubAttribute));
        }

        if ((dynamicScopes.size() > 0) && externalDynamicScopeService.isEnabled()) {
            final UnmodifiableAuthorizationGrant unmodifiableAuthorizationGrant = new UnmodifiableAuthorizationGrant(authorizationGrant);
            DynamicScopeExternalContext dynamicScopeContext = new DynamicScopeExternalContext(dynamicScopes, jwt, unmodifiableAuthorizationGrant);
            externalDynamicScopeService.executeExternalUpdateMethods(dynamicScopeContext);
        }

        // Signature
        String sharedSecret = authorizationGrant.getClient().getClientSecret();
        String signature = cryptoProvider.sign(jwt.getSigningInput(), jwt.getHeader().getKeyId(), sharedSecret, signatureAlgorithm);
        jwt.setEncodedSignature(signature);

        return jwt.toString();
    }

    public String getJweResponse(
            KeyEncryptionAlgorithm keyEncryptionAlgorithm, BlockEncryptionAlgorithm blockEncryptionAlgorithm,
            User user, AuthorizationGrant authorizationGrant, Collection<String> scopes) throws Exception {
        Jwe jwe = new Jwe();

        // Header
        jwe.getHeader().setType(JwtType.JWT);
        jwe.getHeader().setAlgorithm(keyEncryptionAlgorithm);
        jwe.getHeader().setEncryptionMethod(blockEncryptionAlgorithm);

        // Claims
        List<String> dynamicScopes = new ArrayList<String>();
        for (String scopeName : scopes) {
            Scope scope = scopeService.getScopeByDisplayName(scopeName);
            if (org.xdi.oxauth.model.common.ScopeType.DYNAMIC == scope.getScopeType()) {
                dynamicScopes.add(scope.getDisplayName());
                continue;
            }

            if (scope.getOxAuthClaims() != null) {
                for (String claimDn : scope.getOxAuthClaims()) {
                    GluuAttribute gluuAttribute = attributeService.getAttributeByDn(claimDn);

                    String claimName = gluuAttribute.getOxAuthClaimName();
                    String ldapName = gluuAttribute.getName();
                    String attributeValue = null;

                    if (StringUtils.isNotBlank(claimName) && StringUtils.isNotBlank(ldapName)) {
                        if (ldapName.equals("uid")) {
                            attributeValue = user.getUserId();
                        } else {
                            attributeValue = user.getAttribute(gluuAttribute.getName());
                        }

                        jwe.getClaims().setClaim(claimName, attributeValue);
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
                    String ldapClaimName = gluuAttribute.getName();

                    Object attribute = user.getAttribute(ldapClaimName, optional);
                    if (attribute != null) {
                        if (attribute instanceof JSONArray) {
                            JSONArray jsonArray = (JSONArray) attribute;
                            List<String> values = new ArrayList<String>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                String value = jsonArray.optString(i);
                                if (value != null) {
                                    values.add(value);
                                }
                            }
                            jwe.getClaims().setClaim(claim.getName(), values);
                        } else {
                            String value = (String) attribute;
                            jwe.getClaims().setClaim(claim.getName(), value);
                        }
                    }
                }
            }
        }

        // Check for Subject Identifier Type
        if (authorizationGrant.getClient().getSubjectType() != null &&
                SubjectType.fromString(authorizationGrant.getClient().getSubjectType()).equals(SubjectType.PAIRWISE)) {
            String sectorIdentifierUri = null;
            if (StringUtils.isNotBlank(authorizationGrant.getClient().getSectorIdentifierUri())) {
                sectorIdentifierUri = authorizationGrant.getClient().getSectorIdentifierUri();
            } else {
                sectorIdentifierUri = authorizationGrant.getClient().getRedirectUris()[0];
            }

            String userInum = authorizationGrant.getUser().getAttribute("inum");
            PairwiseIdentifier pairwiseIdentifier = pairwiseIdentifierService.findPairWiseIdentifier(
                    userInum, sectorIdentifierUri);
            if (pairwiseIdentifier == null) {
                pairwiseIdentifier = new PairwiseIdentifier(sectorIdentifierUri);
                pairwiseIdentifier.setId(UUID.randomUUID().toString());
                pairwiseIdentifier.setDn(pairwiseIdentifierService.getDnForPairwiseIdentifier(
                        pairwiseIdentifier.getId(),
                        userInum));
                pairwiseIdentifierService.addPairwiseIdentifier(userInum, pairwiseIdentifier);
            }
            jwe.getClaims().setSubjectIdentifier(pairwiseIdentifier.getId());
        } else {
            String openidSubAttribute = appConfiguration.getOpenidSubAttribute();
            jwe.getClaims().setSubjectIdentifier(authorizationGrant.getUser().getAttribute(openidSubAttribute));
        }

        if ((dynamicScopes.size() > 0) && externalDynamicScopeService.isEnabled()) {
            final UnmodifiableAuthorizationGrant unmodifiableAuthorizationGrant = new UnmodifiableAuthorizationGrant(authorizationGrant);
            DynamicScopeExternalContext dynamicScopeContext = new DynamicScopeExternalContext(dynamicScopes, jwe, unmodifiableAuthorizationGrant);
            externalDynamicScopeService.executeExternalUpdateMethods(dynamicScopeContext);
        }

        // Encryption
        if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA_OAEP
                || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA1_5) {
            JSONObject jsonWebKeys = JwtUtil.getJSONWebKeys(authorizationGrant.getClient().getJwksUri());
            AbstractCryptoProvider cryptoProvider = CryptoProviderFactory.getCryptoProvider(appConfiguration);
            String keyId = cryptoProvider.getKeyId(JSONWebKeySet.fromJSONObject(jsonWebKeys), SignatureAlgorithm.RS256);
            PublicKey publicKey = cryptoProvider.getPublicKey(keyId, jsonWebKeys);

            if (publicKey != null) {
                JweEncrypter jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, publicKey);
                jwe = jweEncrypter.encrypt(jwe);
            } else {
                throw new InvalidJweException("The public key is not valid");
            }
        } else if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A128KW
                || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A256KW) {
            try {
                byte[] sharedSymmetricKey = authorizationGrant.getClient().getClientSecret().getBytes(Util.UTF8_STRING_ENCODING);
                JweEncrypter jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, sharedSymmetricKey);
                jwe = jweEncrypter.encrypt(jwe);
            } catch (UnsupportedEncodingException e) {
                throw new InvalidJweException(e);
            } catch (StringEncrypter.EncryptionException e) {
                throw new InvalidJweException(e);
            } catch (Exception e) {
                throw new InvalidJweException(e);
            }
        }

        return jwe.toString();
    }

    /**
     * Builds a JSon String with the response parameters.
     */
    public String getJSonResponse(User user, AuthorizationGrant authorizationGrant, Collection<String> scopes)
            throws Exception {
        JsonWebResponse jsonWebResponse = new JsonWebResponse();

        // Claims
        List<String> dynamicScopes = new ArrayList<String>();
        for (String scopeName : scopes) {
            org.xdi.oxauth.model.common.Scope scope = scopeService.getScopeByDisplayName(scopeName);
            if ((scope != null) && (org.xdi.oxauth.model.common.ScopeType.DYNAMIC == scope.getScopeType())) {
                dynamicScopes.add(scope.getDisplayName());
                continue;
            }

            Map<String, Object> claims = getClaims(user, scope);

            if (scope.getIsOxAuthGroupClaims()) {
                JwtSubClaimObject groupClaim = new JwtSubClaimObject();
                groupClaim.setName(scope.getDisplayName());
                for (Map.Entry<String, Object> entry : claims.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (value instanceof List) {
                        groupClaim.setClaim(key, (List<String>) value);
                    } else {
                        groupClaim.setClaim(key, (String) value);
                    }
                }

                jsonWebResponse.getClaims().setClaim(scope.getDisplayName(), groupClaim);
            } else {
                for (Map.Entry<String, Object> entry : claims.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (value instanceof List) {
                        jsonWebResponse.getClaims().setClaim(key, (List<String>) value);
                    } else {
                        jsonWebResponse.getClaims().setClaim(key, (String) value);
                    }
                }
            }

            jsonWebResponse.getClaims().setSubjectIdentifier(authorizationGrant.getUser().getAttribute("inum"));
        }

        if (authorizationGrant.getJwtAuthorizationRequest() != null
                && authorizationGrant.getJwtAuthorizationRequest().getUserInfoMember() != null) {
            for (Claim claim : authorizationGrant.getJwtAuthorizationRequest().getUserInfoMember().getClaims()) {
                boolean optional = true; // ClaimValueType.OPTIONAL.equals(claim.getClaimValue().getClaimValueType());
                GluuAttribute gluuAttribute = attributeService.getByClaimName(claim.getName());

                if (gluuAttribute != null) {
                    String ldapClaimName = gluuAttribute.getName();

                    Object attribute = user.getAttribute(ldapClaimName, optional);
                    if (attribute != null) {
                        if (attribute instanceof JSONArray) {
                            JSONArray jsonArray = (JSONArray) attribute;
                            List<String> values = new ArrayList<String>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                String value = jsonArray.optString(i);
                                if (value != null) {
                                    values.add(value);
                                }
                            }
                            jsonWebResponse.getClaims().setClaim(claim.getName(), values);
                        } else {
                            String value = (String) attribute;
                            jsonWebResponse.getClaims().setClaim(claim.getName(), value);
                        }
                    }
                }
            }
        }

        // Check for Subject Identifier Type
        if (authorizationGrant.getClient().getSubjectType() != null &&
                SubjectType.fromString(authorizationGrant.getClient().getSubjectType()).equals(SubjectType.PAIRWISE)) {
            String sectorIdentifierUri = null;
            if (StringUtils.isNotBlank(authorizationGrant.getClient().getSectorIdentifierUri())) {
                sectorIdentifierUri = authorizationGrant.getClient().getSectorIdentifierUri();
            } else {
                sectorIdentifierUri = authorizationGrant.getClient().getRedirectUris()[0];
            }

            String userInum = authorizationGrant.getUser().getAttribute("inum");
            PairwiseIdentifier pairwiseIdentifier = pairwiseIdentifierService.findPairWiseIdentifier(
                    userInum, sectorIdentifierUri);
            if (pairwiseIdentifier == null) {
                pairwiseIdentifier = new PairwiseIdentifier(sectorIdentifierUri);
                pairwiseIdentifier.setId(UUID.randomUUID().toString());
                pairwiseIdentifier.setDn(pairwiseIdentifierService.getDnForPairwiseIdentifier(
                        pairwiseIdentifier.getId(),
                        userInum));
                pairwiseIdentifierService.addPairwiseIdentifier(userInum, pairwiseIdentifier);
            }
            jsonWebResponse.getClaims().setSubjectIdentifier(pairwiseIdentifier.getId());
        } else {
            String openidSubAttribute = appConfiguration.getOpenidSubAttribute();
            jsonWebResponse.getClaims().setSubjectIdentifier(authorizationGrant.getUser().getAttribute(openidSubAttribute));
        }

        if ((dynamicScopes.size() > 0) && externalDynamicScopeService.isEnabled()) {
            final UnmodifiableAuthorizationGrant unmodifiableAuthorizationGrant = new UnmodifiableAuthorizationGrant(authorizationGrant);
            DynamicScopeExternalContext dynamicScopeContext = new DynamicScopeExternalContext(dynamicScopes, jsonWebResponse, unmodifiableAuthorizationGrant);
            externalDynamicScopeService.executeExternalUpdateMethods(dynamicScopeContext);
        }

        return jsonWebResponse.toString();
    }

    public Map<String, Object> getClaims(User user, Scope scope) throws InvalidClaimException {
        Map<String, Object> claims = new HashMap<String, Object>();

        if (scope != null && scope.getOxAuthClaims() != null) {
            for (String claimDn : scope.getOxAuthClaims()) {
                GluuAttribute gluuAttribute = attributeService.getAttributeByDn(claimDn);

                String claimName = gluuAttribute.getOxAuthClaimName();
                String ldapName = gluuAttribute.getName();
                Object attribute = null;

                if (StringUtils.isNotBlank(claimName) && StringUtils.isNotBlank(ldapName)) {
                    if (ldapName.equals("uid")) {
                        attribute = user.getUserId();
                    } else {
                        attribute = user.getAttribute(gluuAttribute.getName(), true);
                    }

                    if (attribute != null) {
                        if (attribute instanceof JSONArray) {
                            JSONArray jsonArray = (JSONArray) attribute;
                            List<String> values = new ArrayList<String>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                String value = jsonArray.optString(i);
                                if (value != null) {
                                    values.add(value);
                                }
                            }
                            claims.put(claimName, values);
                        } else {
                            claims.put(claimName, attribute);
                        }
                    }
                }
            }
        }

        return claims;
    }

}