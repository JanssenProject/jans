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
import org.xdi.oxauth.model.common.SubjectType;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.crypto.PublicKey;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.exception.InvalidClaimException;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwe.Jwe;
import org.xdi.oxauth.model.jwe.JweEncrypter;
import org.xdi.oxauth.model.jwe.JweEncrypterImpl;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.jwt.JwtSubClaimObject;
import org.xdi.oxauth.model.jwt.JwtType;
import org.xdi.oxauth.model.ldap.PairwiseIdentifier;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.PairwiseIdentifierService;
import org.xdi.oxauth.service.ScopeService;
import org.xdi.oxauth.service.external.ExternalDynamicScopeService;
import org.xdi.util.security.StringEncrypter;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
 * @version February 15, 2015
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

    @In
    private PairwiseIdentifierService pairwiseIdentifierService;

    public Jwt generateSignedIdToken(IAuthorizationGrant authorizationGrant, String nonce,
                                     AuthorizationCode authorizationCode, AccessToken accessToken,
                                     Set<String> scopes) throws SignatureException, InvalidJwtException,
            StringEncrypter.EncryptionException, InvalidClaimException, NoSuchAlgorithmException, InvalidKeyException {

        JwtSigner jwtSigner = new JwtSigner(authorizationGrant.getClient());
        Jwt jwt = jwtSigner.newJwt();

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
            String codeHash = authorizationCode.getHash(jwtSigner.getSignatureAlgorithm());
            jwt.getClaims().setClaim(JwtClaimName.CODE_HASH, codeHash);
        }
        if (accessToken != null) {
            String accessTokenHash = accessToken.getHash(jwtSigner.getSignatureAlgorithm());
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
                        String attributeValue;

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
                        String attributeValue;

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

        return jwtSigner.sign();
    }

    public Jwe generateEncryptedIdToken(IAuthorizationGrant authorizationGrant, String nonce,
                                        AuthorizationCode authorizationCode, AccessToken accessToken,
                                        Set<String> scopes) throws InvalidJweException, InvalidClaimException, NoSuchAlgorithmException, InvalidKeyException {
        Jwe jwe = new Jwe();

        // Header
        KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm.fromName(authorizationGrant.getClient().getIdTokenEncryptedResponseAlg());
        BlockEncryptionAlgorithm blockEncryptionAlgorithm = BlockEncryptionAlgorithm.fromName(authorizationGrant.getClient().getIdTokenEncryptedResponseEnc());
        jwe.getHeader().setType(JwtType.JWT);
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
                    String attributeValue;

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

        // Check for Subject Identifier Type
        if (authorizationGrant.getClient().getSubjectType() != null &&
                SubjectType.fromString(authorizationGrant.getClient().getSubjectType()).equals(SubjectType.PAIRWISE)) {
            String sectorIdentifier;
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

    public static JsonWebResponse createJwr(
            IAuthorizationGrant grant, String nonce, AuthorizationCode authorizationCode, AccessToken accessToken,
            Set<String> scopes)
            throws InvalidJweException, SignatureException, StringEncrypter.EncryptionException, InvalidJwtException,
            InvalidClaimException, InvalidKeyException, NoSuchAlgorithmException {
        IdTokenFactory idTokenFactory = IdTokenFactory.instance();

        final Client grantClient = grant.getClient();
        if (grantClient != null && grantClient.getIdTokenEncryptedResponseAlg() != null
                && grantClient.getIdTokenEncryptedResponseEnc() != null) {
            return idTokenFactory.generateEncryptedIdToken(grant, nonce, authorizationCode, accessToken, scopes);
        } else {
            return idTokenFactory.generateSignedIdToken(grant, nonce, authorizationCode, accessToken, scopes);
        }
    }

}