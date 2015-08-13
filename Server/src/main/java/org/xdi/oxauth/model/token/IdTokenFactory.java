/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.token;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.model.authorize.Claim;
import org.xdi.oxauth.model.common.AccessToken;
import org.xdi.oxauth.model.common.AuthorizationCode;
import org.xdi.oxauth.model.common.IAuthorizationGrant;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.crypto.PublicKey;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.ECDSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
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
import org.xdi.oxauth.model.jwt.*;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.ScopeService;
import org.xdi.oxauth.service.external.ExternalDynamicScopeService;
import org.xdi.util.security.StringEncrypter;

import java.io.UnsupportedEncodingException;
import java.security.SignatureException;
import java.util.*;

/**
 * JSON Web Token (JWT) is a compact token format intended for space constrained
 * environments such as HTTP Authorization headers and URI query parameters.
 * JWTs encode claims to be transmitted as a JSON object (as defined in RFC
 * 4627) that is base64url encoded and digitally signed. Signing is accomplished
 * using a JSON Web Signature (JWS). JWTs may also be optionally encrypted using
 * JSON Web Encryption (JWE).
 *
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version Jun 22, 2015
 */
@Scope(ScopeType.STATELESS)
@Name("idTokenFactory")
@AutoCreate
public class IdTokenFactory {

    @In
    private ExternalDynamicScopeService externalDynamicScopeService;

    @In
    private ScopeService scopeService;

    @In
    private AttributeService attributeService;

    @In
    private ConfigurationFactory configurationFactory;

    public Jwt generateSignedIdToken(IAuthorizationGrant authorizationGrant, String nonce,
                                     AuthorizationCode authorizationCode, AccessToken accessToken,
                                     Set<String> scopes) throws SignatureException, InvalidJwtException,
            StringEncrypter.EncryptionException, InvalidClaimException {
        Jwt jwt = new Jwt();
        JSONWebKeySet jwks = ConfigurationFactory.instance().getWebKeys();

        // Header
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromName(ConfigurationFactory.instance().getConfiguration().getDefaultSignatureAlgorithm());
        if (authorizationGrant.getClient() != null
                && authorizationGrant.getClient().getIdTokenSignedResponseAlg() != null) {
            signatureAlgorithm = SignatureAlgorithm.fromName(authorizationGrant.getClient().getIdTokenSignedResponseAlg());
        }
        if (signatureAlgorithm == SignatureAlgorithm.NONE) {
            jwt.getHeader().setType(JwtType.JWT);
        } else {
            jwt.getHeader().setType(JwtType.JWS);
        }
        jwt.getHeader().setAlgorithm(signatureAlgorithm);
        List<JSONWebKey> jsonWebKeys = jwks.getKeys(signatureAlgorithm);
        if (jsonWebKeys.size() > 0) {
            jwt.getHeader().setKeyId(jsonWebKeys.get(0).getKeyId());
        }

        // Claims
        jwt.getClaims().setIssuer(ConfigurationFactory.instance().getConfiguration().getIssuer());
        jwt.getClaims().setAudience(authorizationGrant.getClient().getClientId());

        int lifeTime = ConfigurationFactory.instance().getConfiguration().getIdTokenLifetime();
        Calendar calendar = Calendar.getInstance();
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.SECOND, lifeTime);
        Date expiration = calendar.getTime();

        jwt.getClaims().setExpirationTime(expiration);
        jwt.getClaims().setIssuedAt(issuedAt);

        if (authorizationGrant.getAcrValues() != null) {
            jwt.getClaims().setClaim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, authorizationGrant.getAcrValues());
        }
        if (StringUtils.isNotBlank(nonce)) {
            jwt.getClaims().setClaim(JwtClaimName.NONCE, nonce);
        }
        if (authorizationGrant.getAuthenticationTime() != null) {
            jwt.getClaims().setClaim(JwtClaimName.AUTHENTICATION_TIME, authorizationGrant.getAuthenticationTime());
        }
        if (authorizationCode != null) {
            String codeHash = authorizationCode.getHash(signatureAlgorithm);
            jwt.getClaims().setClaim(JwtClaimName.CODE_HASH, codeHash);
        }
        if (accessToken != null) {
            String accessTokenHash = accessToken.getHash(signatureAlgorithm);
            jwt.getClaims().setClaim(JwtClaimName.ACCESS_TOKEN_HASH, accessTokenHash);
        }
        jwt.getClaims().setClaim("oxValidationURI", ConfigurationFactory.instance().getConfiguration().getCheckSessionIFrame());
        jwt.getClaims().setClaim("oxOpenIDConnectVersion", ConfigurationFactory.instance().getConfiguration().getOxOpenIdConnectVersion());

        List<String> dynamicScopes = new ArrayList<String>();
        for (String scopeName : scopes) {
            org.xdi.oxauth.model.common.Scope scope = scopeService.getScopeByDisplayName(scopeName);
            if ((scope != null) && (org.xdi.oxauth.model.common.ScopeType.DYNAMIC == scope.getScopeType())) {
                dynamicScopes.add(scope.getDisplayName());
                continue;
            }

            if (scope != null && scope.getOxAuthClaims() != null) {
                if (scope.getIsOxAuthGroupClaims()) {
                    JwtSubClaimObject groupClaim = new JwtSubClaimObject();
                    groupClaim.setName(scope.getDisplayName());

                    for (String claimDn : scope.getOxAuthClaims()) {
                        GluuAttribute gluuAttribute = attributeService.getAttributeByDn(claimDn);

                        String claimName = gluuAttribute.getOxAuthClaimName();
                        String ldapName = gluuAttribute.getName();
                        String attributeValue = null;

                        if (StringUtils.isNotBlank(claimName) && StringUtils.isNotBlank(ldapName)) {
                            if (ldapName.equals("uid")) {
                                attributeValue = authorizationGrant.getUser().getUserId();
                            } else {
                                attributeValue = authorizationGrant.getUser().getAttribute(gluuAttribute.getName());
                            }

                            groupClaim.setClaim(claimName, attributeValue);
                        }
                    }

                    jwt.getClaims().setClaim(scope.getDisplayName(), groupClaim);
                } else {
                    for (String claimDn : scope.getOxAuthClaims()) {
                        GluuAttribute gluuAttribute = attributeService.getAttributeByDn(claimDn);

                        String claimName = gluuAttribute.getOxAuthClaimName();
                        String ldapName = gluuAttribute.getName();
                        String attributeValue = null;

                        if (StringUtils.isNotBlank(claimName) && StringUtils.isNotBlank(ldapName)) {
                            if (ldapName.equals("uid")) {
                                attributeValue = authorizationGrant.getUser().getUserId();
                            } else {
                                attributeValue = authorizationGrant.getUser().getAttribute(gluuAttribute.getName());
                            }

                            jwt.getClaims().setClaim(claimName, attributeValue);
                        }
                    }
                }
            }
        }
        if (authorizationGrant.getJwtAuthorizationRequest() != null
                && authorizationGrant.getJwtAuthorizationRequest().getIdTokenMember() != null) {
            for (Claim claim : authorizationGrant.getJwtAuthorizationRequest().getIdTokenMember().getClaims()) {
                boolean optional = true; // ClaimValueType.OPTIONAL.equals(claim.getClaimValue().getClaimValueType());
                GluuAttribute gluuAttribute = attributeService.getByClaimName(claim.getName());

                if (gluuAttribute != null) {
                    String ldapClaimName = gluuAttribute.getName();
                    Object attribute = authorizationGrant.getUser().getAttribute(ldapClaimName, optional);
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

        String openidSubAttribute = configurationFactory.getConfiguration().getOpenidSubAttribute();
        jwt.getClaims().setSubjectIdentifier(authorizationGrant.getUser().getAttribute(openidSubAttribute));

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

        return jwt;
    }

    public Jwe generateEncryptedIdToken(IAuthorizationGrant authorizationGrant, String nonce,
                                        AuthorizationCode authorizationCode, AccessToken accessToken,
                                        Set<String> scopes) throws InvalidJweException, InvalidClaimException {
        Jwe jwe = new Jwe();

        // Header
        KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm.fromName(authorizationGrant.getClient().getIdTokenEncryptedResponseAlg());
        BlockEncryptionAlgorithm blockEncryptionAlgorithm = BlockEncryptionAlgorithm.fromName(authorizationGrant.getClient().getIdTokenEncryptedResponseEnc());
        jwe.getHeader().setType(JwtType.JWE);
        jwe.getHeader().setAlgorithm(keyEncryptionAlgorithm);
        jwe.getHeader().setEncryptionMethod(blockEncryptionAlgorithm);

        // Claims
        jwe.getClaims().setIssuer(ConfigurationFactory.instance().getConfiguration().getIssuer());
        jwe.getClaims().setAudience(authorizationGrant.getClient().getClientId());

        int lifeTime = ConfigurationFactory.instance().getConfiguration().getIdTokenLifetime();
        Calendar calendar = Calendar.getInstance();
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.SECOND, lifeTime);
        Date expiration = calendar.getTime();

        jwe.getClaims().setExpirationTime(expiration);
        jwe.getClaims().setIssuedAt(issuedAt);

        String openidSubAttribute = configurationFactory.getConfiguration().getOpenidSubAttribute();
        jwe.getClaims().setSubjectIdentifier(authorizationGrant.getUser().getAttribute(openidSubAttribute));

        if (authorizationGrant.getAcrValues() != null) {
            jwe.getClaims().setClaim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, authorizationGrant.getAcrValues());
        }
        if (StringUtils.isNotBlank(nonce)) {
            jwe.getClaims().setClaim(JwtClaimName.NONCE, nonce);
        }
        if (authorizationGrant.getAuthenticationTime() != null) {
            jwe.getClaims().setClaim(JwtClaimName.AUTHENTICATION_TIME, authorizationGrant.getAuthenticationTime());
        }
        if (authorizationCode != null) {
            String codeHash = authorizationCode.getHash(null);
            jwe.getClaims().setClaim(JwtClaimName.CODE_HASH, codeHash);
        }
        if (accessToken != null) {
            String accessTokenHash = accessToken.getHash(null);
            jwe.getClaims().setClaim(JwtClaimName.ACCESS_TOKEN_HASH, accessTokenHash);
        }
        jwe.getClaims().setClaim("oxValidationURI", ConfigurationFactory.instance().getConfiguration().getCheckSessionIFrame());
        jwe.getClaims().setClaim("oxOpenIDConnectVersion", ConfigurationFactory.instance().getConfiguration().getOxOpenIdConnectVersion());

        List<String> dynamicScopes = new ArrayList<String>();
        for (String scopeName : scopes) {
            org.xdi.oxauth.model.common.Scope scope = scopeService.getScopeByDisplayName(scopeName);
            if (org.xdi.oxauth.model.common.ScopeType.DYNAMIC == scope.getScopeType()) {
                dynamicScopes.add(scope.getDisplayName());
                continue;
            }

            if (scope != null && scope.getOxAuthClaims() != null) {
                for (String claimDn : scope.getOxAuthClaims()) {
                    GluuAttribute gluuAttribute = attributeService.getAttributeByDn(claimDn);

                    String claimName = gluuAttribute.getOxAuthClaimName();
                    String ldapName = gluuAttribute.getName();
                    String attributeValue = null;

                    if (StringUtils.isNotBlank(claimName) && StringUtils.isNotBlank(ldapName)) {
                        if (ldapName.equals("uid")) {
                            attributeValue = authorizationGrant.getUser().getUserId();
                        } else {
                            attributeValue = authorizationGrant.getUser().getAttribute(gluuAttribute.getName());
                        }

                        jwe.getClaims().setClaim(claimName, attributeValue);
                    }
                }
            }
        }

        if (authorizationGrant.getJwtAuthorizationRequest() != null
                && authorizationGrant.getJwtAuthorizationRequest().getIdTokenMember() != null) {
            for (Claim claim : authorizationGrant.getJwtAuthorizationRequest().getIdTokenMember().getClaims()) {
                boolean optional = true; // ClaimValueType.OPTIONAL.equals(claim.getClaimValue().getClaimValueType());
                GluuAttribute gluuAttribute = attributeService.getByClaimName(claim.getName());

                if (gluuAttribute != null) {
                    String ldapClaimName = gluuAttribute.getName();
                    Object attribute = authorizationGrant.getUser().getAttribute(ldapClaimName, optional);
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

        return jwe;
    }

    /**
     * Get IdTokenFactory instance
     *
     * @return IdTokenFactory instance
     */
    public static IdTokenFactory instance() {
        boolean createContexts = !Contexts.isEventContextActive() && !Contexts.isApplicationContextActive();
        if (createContexts) {
            Lifecycle.beginCall();
        }

        return (IdTokenFactory) Component.getInstance(IdTokenFactory.class);
    }

}