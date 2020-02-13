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
import org.gluu.model.GluuAttribute;
import org.gluu.model.attribute.AttributeDataType;
import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.model.custom.script.type.auth.PersonAuthenticationType;
import org.gluu.oxauth.ciba.CIBASupportProxy;
import org.gluu.oxauth.model.authorize.Claim;
import org.gluu.oxauth.model.authorize.JwtAuthorizationRequest;
import org.gluu.oxauth.model.common.*;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.exception.InvalidClaimException;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.jwt.JwtSubClaimObject;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.service.AttributeService;
import org.gluu.oxauth.service.PairwiseIdentifierService;
import org.gluu.oxauth.service.ScopeService;
import org.gluu.oxauth.service.external.ExternalAuthenticationService;
import org.gluu.oxauth.service.external.ExternalDynamicScopeService;
import org.gluu.oxauth.service.external.context.DynamicScopeExternalContext;
import org.gluu.util.StringHelper;
import org.json.JSONArray;
import org.oxauth.persistence.model.PairwiseIdentifier;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.gluu.oxauth.model.common.ScopeType.DYNAMIC;

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
 * @version September 4, 2019
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
    private ScopeService scopeService;

    @Inject
    private AttributeService attributeService;

    @Inject
    private PairwiseIdentifierService pairwiseIdentifierService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private CIBASupportProxy cibaSupportProxy;

    @Inject
    private JwrService jwrService;

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

    private void fillClaims(JsonWebResponse jwr,
                            IAuthorizationGrant authorizationGrant, String nonce,
                            AuthorizationCode authorizationCode, AccessToken accessToken, RefreshToken refreshToken,
                            String state, Set<String> scopes, boolean includeIdTokenClaims, Function<JsonWebResponse, Void> preProcessing) throws Exception {

        jwr.getClaims().setIssuer(appConfiguration.getIssuer());
        jwr.getClaims().setAudience(authorizationGrant.getClient().getClientId());

        int lifeTime = appConfiguration.getIdTokenLifetime();
        Calendar calendar = Calendar.getInstance();
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.SECOND, lifeTime);
        Date expiration = calendar.getTime();

        jwr.getClaims().setExpirationTime(expiration);
        jwr.getClaims().setIssuedAt(issuedAt);

        if (preProcessing != null) {
            preProcessing.apply(jwr);
        }

        if (authorizationGrant.getAcrValues() != null) {
            jwr.setClaim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, authorizationGrant.getAcrValues());
            setAmrClaim(jwr, authorizationGrant.getAcrValues());
        }
        if (StringUtils.isNotBlank(nonce)) {
            jwr.setClaim(JwtClaimName.NONCE, nonce);
        }
        if (authorizationGrant.getAuthenticationTime() != null) {
            jwr.getClaims().setClaim(JwtClaimName.AUTHENTICATION_TIME, authorizationGrant.getAuthenticationTime());
        }
        if (authorizationCode != null) {
            String codeHash = AbstractToken.getHash(authorizationCode.getCode(), jwr.getHeader().getSignatureAlgorithm());
            jwr.setClaim(JwtClaimName.CODE_HASH, codeHash);
        }
        if (accessToken != null) {
            String accessTokenHash = AbstractToken.getHash(accessToken.getCode(), jwr.getHeader().getSignatureAlgorithm());
            jwr.setClaim(JwtClaimName.ACCESS_TOKEN_HASH, accessTokenHash);
        }
        if (Strings.isNotBlank(state)) {
            String stateHash = AbstractToken.getHash(state, jwr.getHeader().getSignatureAlgorithm());
            jwr.setClaim(JwtClaimName.STATE_HASH, stateHash);
        }
        jwr.setClaim(JwtClaimName.OX_OPENID_CONNECT_VERSION, appConfiguration.getOxOpenIdConnectVersion());

        User user = authorizationGrant.getUser();
        List<Scope> dynamicScopes = new ArrayList<Scope>();
        if (includeIdTokenClaims && authorizationGrant.getClient().isIncludeClaimsInIdToken()) {
            for (String scopeName : scopes) {
                Scope scope = scopeService.getScopeById(scopeName);
                if (scope == null) {
                    continue;
                }

                if (DYNAMIC == scope.getScopeType()) {
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
                            groupClaim.setClaim(key, (List) value);
                        } else {
                            groupClaim.setClaim(key, (String) value);
                        }
                    }

                    jwr.getClaims().setClaim(scope.getId(), groupClaim);
                } else {
                    for (Map.Entry<String, Object> entry : claims.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();

                        if (value instanceof List) {
                            jwr.getClaims().setClaim(key, (List) value);
                        } else if (value instanceof Boolean) {
                            jwr.getClaims().setClaim(key, (Boolean) value);
                        } else if (value instanceof Date) {
                            jwr.getClaims().setClaim(key, ((Date) value).getTime());
                        } else {
                            jwr.setClaim(key, (String) value);
                        }
                    }
                }

                jwr.getClaims().setSubjectIdentifier(authorizationGrant.getUser().getAttribute("inum"));
            }
        }

        setClaimsFromJwtAuthorizationRequest(jwr, authorizationGrant, scopes);
        setSubjectIdentifier(jwr, authorizationGrant);

        if ((dynamicScopes.size() > 0) && externalDynamicScopeService.isEnabled()) {
            final UnmodifiableAuthorizationGrant unmodifiableAuthorizationGrant = new UnmodifiableAuthorizationGrant(authorizationGrant);
            DynamicScopeExternalContext dynamicScopeContext = new DynamicScopeExternalContext(dynamicScopes, jwr, unmodifiableAuthorizationGrant);
            externalDynamicScopeService.executeExternalUpdateMethods(dynamicScopeContext);
        }

        processCiba(jwr, authorizationGrant, refreshToken);
    }

    private void processCiba(JsonWebResponse jwr, IAuthorizationGrant authorizationGrant, RefreshToken refreshToken) {
        if (!cibaSupportProxy.isCIBASupported() || !(authorizationGrant instanceof CIBAGrant)) {
            return;
        }

        String refreshTokenHash = AbstractToken.getHash(refreshToken.getCode(), null);
        jwr.setClaim(JwtClaimName.REFRESH_TOKEN_HASH, refreshTokenHash);

        CIBAGrant cibaGrant = (CIBAGrant) authorizationGrant;
        jwr.setClaim(JwtClaimName.AUTH_REQ_ID, cibaGrant.getCIBAAuthenticationRequestId().getCode());
    }

    private void setSubjectIdentifier(JsonWebResponse jwr, IAuthorizationGrant authorizationGrant) throws Exception {
        final Client client = authorizationGrant.getClient();
        if (client.getSubjectType() != null &&
                SubjectType.fromString(client.getSubjectType()).equals(SubjectType.PAIRWISE) &&
                (StringUtils.isNotBlank(client.getSectorIdentifierUri()) || client.getRedirectUris() != null)) {
            String sectorIdentifierUri = null;
            if (StringUtils.isNotBlank(client.getSectorIdentifierUri())) {
                sectorIdentifierUri = client.getSectorIdentifierUri();
            } else {
                sectorIdentifierUri = client.getRedirectUris()[0];
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
            jwr.getClaims().setSubjectIdentifier(pairwiseIdentifier.getId());
        } else {
            if (client.getSubjectType() != null && SubjectType.fromString(client.getSubjectType()).equals(SubjectType.PAIRWISE)) {
                log.warn("Unable to calculate the pairwise subject identifier because the client hasn't a redirect uri. A public subject identifier will be used instead.");
            }
            String openidSubAttribute = appConfiguration.getOpenidSubAttribute();
            String subValue = authorizationGrant.getUser().getAttribute(openidSubAttribute);
            if (StringHelper.equalsIgnoreCase(openidSubAttribute, "uid")) {
                subValue = authorizationGrant.getUser().getUserId();
            }
            jwr.getClaims().setSubjectIdentifier(subValue);
        }
    }

    private void setClaimsFromJwtAuthorizationRequest(JsonWebResponse jwr, IAuthorizationGrant authorizationGrant, Set<String> scopes) throws InvalidClaimException {
        final JwtAuthorizationRequest requestObject = authorizationGrant.getJwtAuthorizationRequest();
        if (requestObject == null || requestObject.getIdTokenMember() == null) {
            return;
        }

        for (Claim claim : requestObject.getIdTokenMember().getClaims()) {
            boolean optional = true; // ClaimValueType.OPTIONAL.equals(claim.getClaimValue().getClaimValueType());
            GluuAttribute gluuAttribute = attributeService.getByClaimName(claim.getName());

            if (gluuAttribute == null) {
                continue;
            }

            Client client = authorizationGrant.getClient();

            if (validateRequesteClaim(gluuAttribute, client.getClaims(), scopes)) {
                String ldapClaimName = gluuAttribute.getName();
                Object attribute = authorizationGrant.getUser().getAttribute(ldapClaimName, optional, gluuAttribute.getOxMultiValuedAttribute());
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
                        jwr.getClaims().setClaim(claim.getName(), values);
                    } else {
                        String value = (String) attribute;
                        jwr.setClaim(claim.getName(), value);
                    }
                }
            }
        }
    }

    public JsonWebResponse createJwr(
            IAuthorizationGrant grant, String nonce,
            AuthorizationCode authorizationCode, AccessToken accessToken, RefreshToken refreshToken,
            String state, Set<String> scopes, boolean includeIdTokenClaims, Function<JsonWebResponse, Void> preProcessing) throws Exception {

        final Client client = grant.getClient();

        JsonWebResponse jwr = jwrService.createJwr(client);
        fillClaims(jwr, grant, nonce, authorizationCode, accessToken, refreshToken, state, scopes, includeIdTokenClaims, preProcessing);
        return jwrService.encode(jwr, client);
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
                Scope scope = scopeService.getScopeById(scopeName);

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
                        attribute = Boolean.parseBoolean(String.valueOf(user.getAttribute(gluuAttribute.getName(), true, gluuAttribute.getOxMultiValuedAttribute())));
                    } else if (AttributeDataType.DATE.equals(gluuAttribute.getDataType())) {
                        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss.SSS'Z'");
                        Object attributeValue = user.getAttribute(gluuAttribute.getName(), true, gluuAttribute.getOxMultiValuedAttribute());
                        if (attributeValue != null) {
                            attribute = format.parse(attributeValue.toString());
                        }
                    } else {
                        attribute = user.getAttribute(gluuAttribute.getName(), true, gluuAttribute.getOxMultiValuedAttribute());
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