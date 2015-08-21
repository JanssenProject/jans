/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.userinfo.ws.rs;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.model.authorize.Claim;
import org.xdi.oxauth.model.common.*;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.crypto.PublicKey;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.ECDSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.exception.InvalidClaimException;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwe.Jwe;
import org.xdi.oxauth.model.jwe.JweEncrypter;
import org.xdi.oxauth.model.jwe.JweEncrypterImpl;
import org.xdi.oxauth.model.jwk.JSONWebKey;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;
import org.xdi.oxauth.model.jws.ECDSASigner;
import org.xdi.oxauth.model.jws.HMACSigner;
import org.xdi.oxauth.model.jws.RSASigner;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtHeaderName;
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
import org.xdi.util.security.StringEncrypter;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.UnsupportedEncodingException;
import java.security.SignatureException;
import java.util.*;

/**
 * Provides interface for User Info REST web services
 *
 * @author Javier Rojas Blum
 * @version August 21, 2015
 */
@Name("requestUserInfoRestWebService")
public class UserInfoRestWebServiceImpl implements UserInfoRestWebService {

    @Logger
    private Log log;

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
    private ConfigurationFactory configurationFactory;

    @In
    private PairwiseIdentifierService pairwiseIdentifierService;

    @Override
    public Response requestUserInfoGet(String accessToken, String authorization, SecurityContext securityContext) {
        return requestUserInfo(accessToken, authorization, securityContext);
    }

    @Override
    public Response requestUserInfoPost(String accessToken, String authorization, SecurityContext securityContext) {
        return requestUserInfo(accessToken, authorization, securityContext);
    }

    public Response requestUserInfo(String accessToken, String authorization, SecurityContext securityContext) {
        if (authorization != null && !authorization.isEmpty() && authorization.startsWith("Bearer ")) {
            accessToken = authorization.substring(7);
        }
        log.debug("Attempting to request User Info, Access token = {0}, Is Secure = {1}",
                accessToken, securityContext.isSecure());
        Response.ResponseBuilder builder = Response.ok();

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
                } else {
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
                        SignatureAlgorithm algorithm = SignatureAlgorithm.fromName(authorizationGrant.getClient().getUserInfoSignedResponseAlg());
                        builder.type("application/jwt");
                        builder.entity(getJwtResponse(algorithm,
                                currentUser,
                                authorizationGrant,
                                authorizationGrant.getScopes()));
                    } else {
                        builder.type((MediaType.APPLICATION_JSON));
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

        return builder.build();
    }

    public String getJwtResponse(SignatureAlgorithm signatureAlgorithm, User user, AuthorizationGrant authorizationGrant,
                                 Collection<String> scopes) throws StringEncrypter.EncryptionException, InvalidJwtException, InvalidClaimException, SignatureException {
        Jwt jwt = new Jwt();
        JSONWebKeySet jwks = ConfigurationFactory.instance().getWebKeys();

        // Header
        if (signatureAlgorithm == SignatureAlgorithm.NONE) {
            jwt.getHeader().setType(JwtType.JWT);
        } else {
            jwt.getHeader().setType(JwtType.JWS);
        }
        jwt.getHeader().setAlgorithm(signatureAlgorithm);

        List<JSONWebKey> availableKeys = jwks.getKeys(signatureAlgorithm);
        if (availableKeys.size() > 0) {
            jwt.getHeader().setKeyId(availableKeys.get(0).getKeyId());
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
            String sectorIdentifier = null;
            if (StringUtils.isNotBlank(authorizationGrant.getClient().getSectorIdentifierUri())) {
                sectorIdentifier = authorizationGrant.getClient().getSectorIdentifierUri();
            } else {
                sectorIdentifier = authorizationGrant.getClient().getRedirectUris()[0];
            }

            String userInum = authorizationGrant.getUser().getAttribute("inum");
            PairwiseIdentifier pairwiseIdentifier = pairwiseIdentifierService.findPairWiseIdentifier(
                    userInum, sectorIdentifier);
            if (pairwiseIdentifier == null) {
                pairwiseIdentifier = new PairwiseIdentifier(sectorIdentifier);
                pairwiseIdentifier.setId(UUID.randomUUID().toString());
                pairwiseIdentifier.setDn(pairwiseIdentifierService.getDnForPairwiseIdentifier(
                        pairwiseIdentifier.getId(),
                        userInum));
                pairwiseIdentifierService.addPairwiseIdentifier(userInum, pairwiseIdentifier);
            }
            jwt.getClaims().setSubjectIdentifier(pairwiseIdentifier.getId());
        } else {
            String openidSubAttribute = configurationFactory.getConfiguration().getOpenidSubAttribute();
            jwt.getClaims().setSubjectIdentifier(authorizationGrant.getUser().getAttribute(openidSubAttribute));
        }

        if ((dynamicScopes.size() > 0) && externalDynamicScopeService.isEnabled()) {
            externalDynamicScopeService.executeExternalUpdateMethods(dynamicScopes, jwt, authorizationGrant.getUser());
        }

        // Signature
        JSONWebKey jwk = null;
        switch (signatureAlgorithm) {
            case HS256:
            case HS384:
            case HS512:
                HMACSigner hmacSigner = new HMACSigner(signatureAlgorithm, authorizationGrant.getClient().getClientSecret());
                jwt = hmacSigner.sign(jwt);
                break;
            case RS256:
            case RS384:
            case RS512:
                jwk = jwks.getKey(jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
                RSAPrivateKey rsaPrivateKey = new RSAPrivateKey(
                        jwk.getPrivateKey().getModulus(),
                        jwk.getPrivateKey().getPrivateExponent());
                RSASigner rsaSigner = new RSASigner(signatureAlgorithm, rsaPrivateKey);
                jwt = rsaSigner.sign(jwt);
                break;
            case ES256:
            case ES384:
            case ES512:
                jwk = jwks.getKey(jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
                ECDSAPrivateKey ecdsaPrivateKey = new ECDSAPrivateKey(jwk.getPrivateKey().getD());
                ECDSASigner ecdsaSigner = new ECDSASigner(signatureAlgorithm, ecdsaPrivateKey);
                jwt = ecdsaSigner.sign(jwt);
                break;
            case NONE:
                break;
            default:
                break;
        }

        return jwt.toString();
    }

    public String getJweResponse(KeyEncryptionAlgorithm keyEncryptionAlgorithm, BlockEncryptionAlgorithm blockEncryptionAlgorithm,
                                 User user, AuthorizationGrant authorizationGrant, Collection<String> scopes)
            throws InvalidClaimException, InvalidJweException {
        Jwe jwe = new Jwe();

        // Header
        jwe.getHeader().setType(JwtType.JWE);
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
            String sectorIdentifier = null;
            if (StringUtils.isNotBlank(authorizationGrant.getClient().getSectorIdentifierUri())) {
                sectorIdentifier = authorizationGrant.getClient().getSectorIdentifierUri();
            } else {
                sectorIdentifier = authorizationGrant.getClient().getRedirectUris()[0];
            }

            String userInum = authorizationGrant.getUser().getAttribute("inum");
            PairwiseIdentifier pairwiseIdentifier = pairwiseIdentifierService.findPairWiseIdentifier(
                    userInum, sectorIdentifier);
            if (pairwiseIdentifier == null) {
                pairwiseIdentifier = new PairwiseIdentifier(sectorIdentifier);
                pairwiseIdentifier.setId(UUID.randomUUID().toString());
                pairwiseIdentifier.setDn(pairwiseIdentifierService.getDnForPairwiseIdentifier(
                        pairwiseIdentifier.getId(),
                        userInum));
                pairwiseIdentifierService.addPairwiseIdentifier(userInum, pairwiseIdentifier);
            }
            jwe.getClaims().setSubjectIdentifier(pairwiseIdentifier.getId());
        } else {
            String openidSubAttribute = configurationFactory.getConfiguration().getOpenidSubAttribute();
            jwe.getClaims().setSubjectIdentifier(authorizationGrant.getUser().getAttribute(openidSubAttribute));
        }

        if ((dynamicScopes.size() > 0) && externalDynamicScopeService.isEnabled()) {
            externalDynamicScopeService.executeExternalUpdateMethods(dynamicScopes, jwe, authorizationGrant.getUser());
        }

        // Encryption
        if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA_OAEP
                || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA1_5) {
            PublicKey publicKey = JwtUtil.getPublicKey(authorizationGrant.getClient().getJwksUri(), null, SignatureAlgorithm.RS256, null);
            if (publicKey != null && publicKey instanceof RSAPublicKey) {
                JweEncrypter jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, (RSAPublicKey) publicKey);
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
            throws JSONException, InvalidClaimException {
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
            String sectorIdentifier = null;
            if (StringUtils.isNotBlank(authorizationGrant.getClient().getSectorIdentifierUri())) {
                sectorIdentifier = authorizationGrant.getClient().getSectorIdentifierUri();
            } else {
                sectorIdentifier = authorizationGrant.getClient().getRedirectUris()[0];
            }

            String userInum = authorizationGrant.getUser().getAttribute("inum");
            PairwiseIdentifier pairwiseIdentifier = pairwiseIdentifierService.findPairWiseIdentifier(
                    userInum, sectorIdentifier);
            if (pairwiseIdentifier == null) {
                pairwiseIdentifier = new PairwiseIdentifier(sectorIdentifier);
                pairwiseIdentifier.setId(UUID.randomUUID().toString());
                pairwiseIdentifier.setDn(pairwiseIdentifierService.getDnForPairwiseIdentifier(
                        pairwiseIdentifier.getId(),
                        userInum));
                pairwiseIdentifierService.addPairwiseIdentifier(userInum, pairwiseIdentifier);
            }
            jsonWebResponse.getClaims().setSubjectIdentifier(pairwiseIdentifier.getId());
        } else {
            String openidSubAttribute = configurationFactory.getConfiguration().getOpenidSubAttribute();
            jsonWebResponse.getClaims().setSubjectIdentifier(authorizationGrant.getUser().getAttribute(openidSubAttribute));
        }

        if ((dynamicScopes.size() > 0) && externalDynamicScopeService.isEnabled()) {
            externalDynamicScopeService.executeExternalUpdateMethods(dynamicScopes, jsonWebResponse, authorizationGrant.getUser());
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