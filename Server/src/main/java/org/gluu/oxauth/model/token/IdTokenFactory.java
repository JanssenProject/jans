/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.token;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.model.GluuAttribute;
import org.gluu.model.attribute.AttributeDataType;
import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.model.custom.script.type.auth.PersonAuthenticationType;
import org.gluu.oxauth.model.authorize.Claim;
import org.gluu.oxauth.model.common.*;
import org.gluu.oxauth.model.config.WebKeysConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.crypto.CryptoProviderFactory;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.exception.InvalidClaimException;
import org.gluu.oxauth.model.exception.InvalidJweException;
import org.gluu.oxauth.model.jwe.Jwe;
import org.gluu.oxauth.model.jwe.JweEncrypter;
import org.gluu.oxauth.model.jwe.JweEncrypterImpl;
import org.gluu.oxauth.model.jwk.Algorithm;
import org.gluu.oxauth.model.jwk.JSONWebKeySet;
import org.gluu.oxauth.model.jwk.Use;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.jwt.JwtSubClaimObject;
import org.gluu.oxauth.model.jwt.JwtType;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.util.JwtUtil;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxauth.service.AttributeService;
import org.gluu.oxauth.service.ClientService;
import org.gluu.oxauth.service.PairwiseIdentifierService;
import org.gluu.oxauth.service.ScopeService;
import org.gluu.oxauth.service.external.ExternalAuthenticationService;
import org.gluu.oxauth.service.external.ExternalDynamicScopeService;
import org.gluu.oxauth.service.external.context.DynamicScopeExternalContext;
import org.gluu.util.security.StringEncrypter;
import org.oxauth.persistence.model.PairwiseIdentifier;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.UnsupportedEncodingException;
import java.security.PublicKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
 * @version May 3, 2019
 */
@Stateless
@Named
public class IdTokenFactory {

    @Inject
    private Logger log;

    @Inject
    private ExternalDynamicScopeService externalDynamicScopeService;

    @Inject
    private ExternalAuthenticationService externalAuthenticationService;

    @Inject
    private ClientService clientService;

    @Inject
    private ScopeService scopeService;

    @Inject
    private AttributeService attributeService;

    @Inject
    private PairwiseIdentifierService pairwiseIdentifierService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    public Jwt generateSignedIdToken(IAuthorizationGrant authorizationGrant, String nonce,
                                     AuthorizationCode authorizationCode, AccessToken accessToken, String state,
                                     Set<String> scopes, boolean includeIdTokenClaims, Function<JsonWebResponse, Void> preProcessing) throws Exception {

        JwtSigner jwtSigner = JwtSigner.newJwtSigner(appConfiguration, webKeysConfiguration, authorizationGrant.getClient());
        Jwt jwt = jwtSigner.newJwt();

        int lifeTime = appConfiguration.getIdTokenLifetime();
        Calendar calendar = Calendar.getInstance();
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.SECOND, lifeTime);
        Date expiration = calendar.getTime();

        jwt.getClaims().setExpirationTime(expiration);
        jwt.getClaims().setIssuedAt(issuedAt);

        if (preProcessing != null) {
            preProcessing.apply(jwt);
        }

        if (authorizationGrant.getAcrValues() != null) {
            jwt.getClaims().setClaim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, authorizationGrant.getAcrValues());
            setAmrClaim(jwt, authorizationGrant.getAcrValues());
        }
        if (StringUtils.isNotBlank(nonce)) {
            jwt.getClaims().setClaim(JwtClaimName.NONCE, nonce);
        }
        if (authorizationGrant.getAuthenticationTime() != null) {
            jwt.getClaims().setClaim(JwtClaimName.AUTHENTICATION_TIME, authorizationGrant.getAuthenticationTime());
        }
        if (authorizationCode != null) {
            String codeHash = AbstractToken.getHash(authorizationCode.getCode(), jwtSigner.getSignatureAlgorithm());
            jwt.getClaims().setClaim(JwtClaimName.CODE_HASH, codeHash);
        }
        if (accessToken != null) {
            String accessTokenHash = AbstractToken.getHash(accessToken.getCode(), jwtSigner.getSignatureAlgorithm());
            jwt.getClaims().setClaim(JwtClaimName.ACCESS_TOKEN_HASH, accessTokenHash);
        }
        if (Strings.isNotBlank(state)) {
            String stateHash = AbstractToken.getHash(state, jwtSigner.getSignatureAlgorithm());
            jwt.getClaims().setClaim(JwtClaimName.STATE_HASH, stateHash);
        }
        jwt.getClaims().setClaim(JwtClaimName.OX_OPENID_CONNECT_VERSION, appConfiguration.getOxOpenIdConnectVersion());

        User user = authorizationGrant.getUser();
        List<Scope> dynamicScopes = new ArrayList<Scope>();
        if (includeIdTokenClaims && authorizationGrant.getClient().isIncludeClaimsInIdToken()) {
            for (String scopeName : scopes) {
                org.oxauth.persistence.model.Scope scope = scopeService.getScopeById(scopeName);
                if ((scope != null) && (org.gluu.oxauth.model.common.ScopeType.DYNAMIC == scope.getScopeType())) {
                    dynamicScopes.add(scope);
                    continue;
                }

                Map<String, Object> claims = getClaims(user, scope);

                if (Boolean.TRUE.equals(scope.isOxAuthGroupClaims())) {
                    JwtSubClaimObject groupClaim = new JwtSubClaimObject();
                    groupClaim.setName(scope.getId());
                    for (Map.Entry<String, Object> entry : claims.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();

                        if (value instanceof List) {
                            groupClaim.setClaim(key, (List<String>) value);
                        } else {
                            groupClaim.setClaim(key, (String) value);
                        }
                    }

                    jwt.getClaims().setClaim(scope.getId(), groupClaim);
                } else {
                    for (Map.Entry<String, Object> entry : claims.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();

                        if (value instanceof List) {
                            jwt.getClaims().setClaim(key, (List<String>) value);
                        } else if (value instanceof Boolean) {
                            jwt.getClaims().setClaim(key, (Boolean) value);
                        } else if (value instanceof Date) {
                            jwt.getClaims().setClaim(key, ((Date) value).getTime());
                        } else {
                            jwt.getClaims().setClaim(key, (String) value);
                        }
                    }
                }

                jwt.getClaims().setSubjectIdentifier(authorizationGrant.getUser().getAttribute("inum"));
            }
        }

        if (authorizationGrant.getJwtAuthorizationRequest() != null
                && authorizationGrant.getJwtAuthorizationRequest().getIdTokenMember() != null) {
            for (Claim claim : authorizationGrant.getJwtAuthorizationRequest().getIdTokenMember().getClaims()) {
                boolean optional = true; // ClaimValueType.OPTIONAL.equals(claim.getClaimValue().getClaimValueType());
                GluuAttribute gluuAttribute = attributeService.getByClaimName(claim.getName());

                if (gluuAttribute != null) {
                    Client client = authorizationGrant.getClient();

                    if (validateRequesteClaim(gluuAttribute, client.getClaims(), scopes)) {
                        String ldapClaimName = gluuAttribute.getName();
                        Object attribute = authorizationGrant.getUser().getAttribute(ldapClaimName, optional, gluuAttribute.getOxMultivaluedAttribute());
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
        }

        // Check for Subject Identifier Type
        if (authorizationGrant.getClient().getSubjectType() != null &&
                SubjectType.fromString(authorizationGrant.getClient().getSubjectType()).equals(SubjectType.PAIRWISE) &&
                (StringUtils.isNotBlank(authorizationGrant.getClient().getSectorIdentifierUri()) || authorizationGrant.getClient().getRedirectUris() != null)) {
            String sectorIdentifierUri = null;
            if (StringUtils.isNotBlank(authorizationGrant.getClient().getSectorIdentifierUri())) {
                sectorIdentifierUri = authorizationGrant.getClient().getSectorIdentifierUri();
            } else {
                sectorIdentifierUri = authorizationGrant.getClient().getRedirectUris()[0];
            }

            String userInum = authorizationGrant.getUser().getAttribute("inum");
            String clientId = authorizationGrant.getClientId();
            PairwiseIdentifier pairwiseIdentifier = pairwiseIdentifierService.findPairWiseIdentifier(
                    userInum, sectorIdentifierUri, clientId);
            if (pairwiseIdentifier == null) {
                pairwiseIdentifier = new PairwiseIdentifier(sectorIdentifierUri, clientId);
                pairwiseIdentifier.setId(UUID.randomUUID().toString());
                pairwiseIdentifier.setDn(pairwiseIdentifierService.getDnForPairwiseIdentifier(
                        pairwiseIdentifier.getId(),
                        userInum));
                pairwiseIdentifierService.addPairwiseIdentifier(userInum, pairwiseIdentifier);
            }
            jwt.getClaims().setSubjectIdentifier(pairwiseIdentifier.getId());
        } else {
            if (authorizationGrant.getClient().getSubjectType() != null && SubjectType.fromString(authorizationGrant.getClient().getSubjectType()).equals(SubjectType.PAIRWISE)) {
                log.warn("Unable to calculate the pairwise subject identifier because the client hasn't a redirect uri. A public subject identifier will be used instead.");
            }

            String openidSubAttribute = appConfiguration.getOpenidSubAttribute();
            jwt.getClaims().setSubjectIdentifier(authorizationGrant.getUser().getAttribute(openidSubAttribute));
        }

        if ((dynamicScopes.size() > 0) && externalDynamicScopeService.isEnabled()) {
            final UnmodifiableAuthorizationGrant unmodifiableAuthorizationGrant = new UnmodifiableAuthorizationGrant(authorizationGrant);
            DynamicScopeExternalContext dynamicScopeContext = new DynamicScopeExternalContext(dynamicScopes, jwt, unmodifiableAuthorizationGrant);
            externalDynamicScopeService.executeExternalUpdateMethods(dynamicScopeContext);
        }

        return jwtSigner.sign();
    }

    private void setAmrClaim(JsonWebResponse jwt, String acrValues) {
        List<String> amrList = Lists.newArrayList();

        CustomScriptConfiguration script = externalAuthenticationService.getCustomScriptConfigurationByName(acrValues);
        if (script != null) {
            amrList.add(Integer.toString(script.getLevel()));

            PersonAuthenticationType externalAuthenticator = (PersonAuthenticationType) script.getExternalType();
            int apiVersion = externalAuthenticator.getApiVersion();

            if (apiVersion > 3) {
                Map<String, String> authenticationMethodClaimsOrNull = externalAuthenticator.getAuthenticationMethodClaims();
                if (authenticationMethodClaimsOrNull != null) {
                    for (String key : authenticationMethodClaimsOrNull.keySet()) {
                        amrList.add(key + ":" + authenticationMethodClaimsOrNull.get(key));
                    }
                }
            }
        }

        jwt.getClaims().setClaim(JwtClaimName.AUTHENTICATION_METHOD_REFERENCES, amrList);
    }

    public Jwe generateEncryptedIdToken(IAuthorizationGrant authorizationGrant, String nonce,
                                        AuthorizationCode authorizationCode, AccessToken accessToken, String state,
                                        Set<String> scopes, boolean includeIdTokenClaims, Function<JsonWebResponse, Void> preProcessing) throws Exception {
        Jwe jwe = new Jwe();

        // Header
        KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm.fromName(authorizationGrant.getClient().getIdTokenEncryptedResponseAlg());
        BlockEncryptionAlgorithm blockEncryptionAlgorithm = BlockEncryptionAlgorithm.fromName(authorizationGrant.getClient().getIdTokenEncryptedResponseEnc());
        jwe.getHeader().setType(JwtType.JWT);
        jwe.getHeader().setAlgorithm(keyEncryptionAlgorithm);
        jwe.getHeader().setEncryptionMethod(blockEncryptionAlgorithm);

        // Claims
        jwe.getClaims().setIssuer(appConfiguration.getIssuer());
        jwe.getClaims().setAudience(authorizationGrant.getClient().getClientId());

        int lifeTime = appConfiguration.getIdTokenLifetime();
        Calendar calendar = Calendar.getInstance();
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.SECOND, lifeTime);
        Date expiration = calendar.getTime();

        jwe.getClaims().setExpirationTime(expiration);
        jwe.getClaims().setIssuedAt(issuedAt);

        if (preProcessing != null) {
            preProcessing.apply(jwe);
        }

        if (authorizationGrant.getAcrValues() != null) {
            jwe.getClaims().setClaim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, authorizationGrant.getAcrValues());
            setAmrClaim(jwe, authorizationGrant.getAcrValues());
        }
        if (StringUtils.isNotBlank(nonce)) {
            jwe.getClaims().setClaim(JwtClaimName.NONCE, nonce);
        }
        if (authorizationGrant.getAuthenticationTime() != null) {
            jwe.getClaims().setClaim(JwtClaimName.AUTHENTICATION_TIME, authorizationGrant.getAuthenticationTime());
        }
        if (authorizationCode != null) {
            String codeHash = authorizationCode.getHash(authorizationCode.getCode(), null);
            jwe.getClaims().setClaim(JwtClaimName.CODE_HASH, codeHash);
        }
        if (accessToken != null) {
            String accessTokenHash = accessToken.getHash(accessToken.getCode(), null);
            jwe.getClaims().setClaim(JwtClaimName.ACCESS_TOKEN_HASH, accessTokenHash);
        }
        if (Strings.isNotBlank(state)) {
            String stateHash = AbstractToken.getHash(state, null);
            jwe.getClaims().setClaim(JwtClaimName.STATE_HASH, stateHash);
        }
        jwe.getClaims().setClaim(JwtClaimName.OX_OPENID_CONNECT_VERSION, appConfiguration.getOxOpenIdConnectVersion());

        User user = authorizationGrant.getUser();
        List<Scope> dynamicScopes = new ArrayList<Scope>();
        if (includeIdTokenClaims && authorizationGrant.getClient().isIncludeClaimsInIdToken()) {
            for (String scopeName : scopes) {
                org.oxauth.persistence.model.Scope scope = scopeService.getScopeById(scopeName);
                if ((scope != null) && (org.gluu.oxauth.model.common.ScopeType.DYNAMIC == scope.getScopeType())) {
                    dynamicScopes.add(scope);
                    continue;
                }

                Map<String, Object> claims = getClaims(user, scope);

                if (Boolean.TRUE.equals(scope.isOxAuthGroupClaims())) {
                    JwtSubClaimObject groupClaim = new JwtSubClaimObject();
                    groupClaim.setName(scope.getId());
                    for (Map.Entry<String, Object> entry : claims.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();

                        if (value instanceof List) {
                            groupClaim.setClaim(key, (List<String>) value);
                        } else {
                            groupClaim.setClaim(key, (String) value);
                        }
                    }

                    jwe.getClaims().setClaim(scope.getId(), groupClaim);
                } else {
                    for (Map.Entry<String, Object> entry : claims.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();

                        if (value instanceof List) {
                            jwe.getClaims().setClaim(key, (List<String>) value);
                        } else if (value instanceof Boolean) {
                            jwe.getClaims().setClaim(key, (Boolean) value);
                        } else if (value instanceof Date) {
                            jwe.getClaims().setClaim(key, ((Date) value).getTime());
                        } else {
                            jwe.getClaims().setClaim(key, (String) value);
                        }
                    }
                }

                jwe.getClaims().setSubjectIdentifier(authorizationGrant.getUser().getAttribute("inum"));
            }
        }

        if (authorizationGrant.getJwtAuthorizationRequest() != null
                && authorizationGrant.getJwtAuthorizationRequest().getIdTokenMember() != null) {
            for (Claim claim : authorizationGrant.getJwtAuthorizationRequest().getIdTokenMember().getClaims()) {
                boolean optional = true; // ClaimValueType.OPTIONAL.equals(claim.getClaimValue().getClaimValueType());
                GluuAttribute gluuAttribute = attributeService.getByClaimName(claim.getName());

                if (gluuAttribute != null) {
                    Client client = authorizationGrant.getClient();

                    if (validateRequesteClaim(gluuAttribute, client.getClaims(), scopes)) {
                        String ldapClaimName = gluuAttribute.getName();
                        Object attribute = authorizationGrant.getUser().getAttribute(ldapClaimName, optional, gluuAttribute.getOxMultivaluedAttribute());
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
        }

        // Check for Subject Identifier Type
        if (authorizationGrant.getClient().getSubjectType() != null &&
                SubjectType.fromString(authorizationGrant.getClient().getSubjectType()).equals(SubjectType.PAIRWISE) &&
                (StringUtils.isNotBlank(authorizationGrant.getClient().getSectorIdentifierUri()) || authorizationGrant.getClient().getRedirectUris() != null)) {
            String sectorIdentifierUri = null;
            if (StringUtils.isNotBlank(authorizationGrant.getClient().getSectorIdentifierUri())) {
                sectorIdentifierUri = authorizationGrant.getClient().getSectorIdentifierUri();
            } else {
                sectorIdentifierUri = authorizationGrant.getClient().getRedirectUris()[0];
            }

            String userInum = authorizationGrant.getUser().getAttribute("inum");
            String clientId = authorizationGrant.getClientId();
            PairwiseIdentifier pairwiseIdentifier = pairwiseIdentifierService.findPairWiseIdentifier(
                    userInum, sectorIdentifierUri, clientId);
            if (pairwiseIdentifier == null) {
                pairwiseIdentifier = new PairwiseIdentifier(sectorIdentifierUri, clientId);
                pairwiseIdentifier.setId(UUID.randomUUID().toString());
                pairwiseIdentifier.setDn(pairwiseIdentifierService.getDnForPairwiseIdentifier(
                        pairwiseIdentifier.getId(),
                        userInum));
                pairwiseIdentifierService.addPairwiseIdentifier(userInum, pairwiseIdentifier);
            }
            jwe.getClaims().setSubjectIdentifier(pairwiseIdentifier.getId());
        } else {
            if (authorizationGrant.getClient().getSubjectType() != null && SubjectType.fromString(authorizationGrant.getClient().getSubjectType()).equals(SubjectType.PAIRWISE)) {
                log.warn("Unable to calculate the pairwise subject identifier because the client hasn't a redirect uri. A public subject identifier will be used instead.");
            }

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
            String keyId = cryptoProvider.getKeyId(JSONWebKeySet.fromJSONObject(jsonWebKeys),
                    Algorithm.fromString(keyEncryptionAlgorithm.getName()),
                    Use.ENCRYPTION);
            PublicKey publicKey = cryptoProvider.getPublicKey(keyId, jsonWebKeys);
            jwe.getHeader().setKeyId(keyId);

            if (publicKey != null) {
                JweEncrypter jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, publicKey);
                jwe = jweEncrypter.encrypt(jwe);
            } else {
                throw new InvalidJweException("The public key is not valid");
            }
        } else if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A128KW
                || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A256KW) {
            try {
                byte[] sharedSymmetricKey = clientService.decryptSecret(authorizationGrant.getClient().getClientSecret()).getBytes(Util.UTF8_STRING_ENCODING);
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

    public JsonWebResponse createJwr(IAuthorizationGrant grant, String nonce,
                                     AuthorizationCode authorizationCode, AccessToken accessToken, String state,
                                     Set<String> scopes, boolean includeIdTokenClaims, Function<JsonWebResponse, Void> preProcessing) throws Exception {
        final Client grantClient = grant.getClient();
        if (grantClient != null && grantClient.getIdTokenEncryptedResponseAlg() != null
                && grantClient.getIdTokenEncryptedResponseEnc() != null) {
            return generateEncryptedIdToken(
                    grant, nonce, authorizationCode, accessToken, state, scopes, includeIdTokenClaims, preProcessing);
        } else {
            return generateSignedIdToken(
                    grant, nonce, authorizationCode, accessToken, state, scopes, includeIdTokenClaims, preProcessing);
        }
    }

    public boolean validateRequesteClaim(GluuAttribute gluuAttribute, String[] clientAllowedClaims, Collection<String> scopes) {
        if (gluuAttribute != null) {
            if (clientAllowedClaims != null) {
                for (int i = 0; i < clientAllowedClaims.length; i++) {
                    if (gluuAttribute.getDn().equals(clientAllowedClaims[i])) {
                        return true;
                    }
                }
            }

            for (String scopeName : scopes) {
                org.oxauth.persistence.model.Scope scope = scopeService.getScopeById(scopeName);

                if (scope != null && scope.getOxAuthClaims() != null) {
                    for (String claimDn : scope.getOxAuthClaims()) {
                        if (gluuAttribute.getDisplayName().equals(attributeService.getAttributeByDn(claimDn).getDisplayName())) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public Map<String, Object> getClaims(User user, Scope scope) throws InvalidClaimException, ParseException {
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
                    } else if (AttributeDataType.BOOLEAN.equals(gluuAttribute.getDataType())) {
                        attribute = Boolean.parseBoolean((String) user.getAttribute(gluuAttribute.getName(), true, gluuAttribute.getOxMultivaluedAttribute()));
                    } else if (AttributeDataType.DATE.equals(gluuAttribute.getDataType())) {
                        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss.SSS'Z'");
                        Object attributeValue = user.getAttribute(gluuAttribute.getName(), true, gluuAttribute.getOxMultivaluedAttribute());
                        if (attributeValue != null) {
                            attribute = format.parse(attributeValue.toString());
                        }
                    } else {
                        attribute = user.getAttribute(gluuAttribute.getName(), true, gluuAttribute.getOxMultivaluedAttribute());
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