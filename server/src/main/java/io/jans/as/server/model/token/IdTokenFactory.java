/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.token;

import com.google.common.collect.Lists;
import io.jans.as.common.claims.Audience;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.exception.InvalidClaimException;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.jwt.JwtSubClaimObject;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.model.authorize.Claim;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.as.server.model.common.AbstractToken;
import io.jans.as.server.model.common.AccessToken;
import io.jans.as.server.model.common.AuthorizationCode;
import io.jans.as.server.model.common.CIBAGrant;
import io.jans.as.server.model.common.IAuthorizationGrant;
import io.jans.as.server.model.common.RefreshToken;
import io.jans.as.server.model.common.SessionId;
import io.jans.as.server.model.common.UnmodifiableAuthorizationGrant;
import io.jans.as.server.service.ScopeService;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.service.external.ExternalDynamicScopeService;
import io.jans.as.server.service.external.context.DynamicScopeExternalContext;
import io.jans.model.GluuAttribute;
import io.jans.model.attribute.AttributeDataType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.auth.PersonAuthenticationType;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static io.jans.as.model.common.ScopeType.DYNAMIC;

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
 * @author Yuriy Zabrovarnyy
 * @version 12 Feb, 2020
 */
@ApplicationScoped
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
    private AppConfiguration appConfiguration;

    @Inject
    private JwrService jwrService;

    @Inject
    private SessionIdService sessionIdService;

    private void setAmrClaim(JsonWebResponse jwt, String acrValues) {
        List<String> amrList = Lists.newArrayList();

        CustomScriptConfiguration script = externalAuthenticationService.getCustomScriptConfigurationByName(acrValues);
        if (script != null) {
            amrList.add(Integer.toString(script.getLevel()));

            PersonAuthenticationType externalAuthenticator = (PersonAuthenticationType) script.getExternalType();
            int apiVersion = externalAuthenticator.getApiVersion();

            if (apiVersion > 3) {
                Map<String, String> authenticationMethodClaimsOrNull = externalAuthenticator.getAuthenticationMethodClaims(script.getConfigurationAttributes());
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
                            String state, Set<String> scopes, boolean includeIdTokenClaims, Function<JsonWebResponse,
                            Void> preProcessing, Function<JsonWebResponse, Void> postProcessing, String requestedClaims) throws Exception {

        jwr.getClaims().setIssuer(appConfiguration.getIssuer());
        Audience.setAudience(jwr.getClaims(), authorizationGrant.getClient());

        int lifeTime = appConfiguration.getIdTokenLifetime();
        Calendar calendar = Calendar.getInstance();
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.SECOND, lifeTime);
        Date expiration = calendar.getTime();

        jwr.getClaims().setExpirationTime(expiration);
        jwr.getClaims().setIssuedAt(issuedAt);
        jwr.setClaim("code", UUID.randomUUID().toString());

        if (preProcessing != null) {
            preProcessing.apply(jwr);
        }
        final SessionId session = sessionIdService.getSessionByDn(authorizationGrant.getSessionDn());
        if (session != null) {
            jwr.setClaim("sid", session.getOutsideSid());
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
        if (authorizationGrant.getGrantType() != null) {
            jwr.setClaim("grant", authorizationGrant.getGrantType().getValue());
        }
        jwr.setClaim(JwtClaimName.OX_OPENID_CONNECT_VERSION, appConfiguration.getOxOpenIdConnectVersion());

        User user = authorizationGrant.getUser();
        List<Scope> dynamicScopes = new ArrayList<>();
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

                if (Boolean.TRUE.equals(scope.isGroupClaims())) {
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
        setClaimsFromRequestedClaims(requestedClaims, jwr, user);
        filterClaimsBasedOnAccessToken(jwr, accessToken, authorizationCode);
        jwrService.setSubjectIdentifier(jwr, authorizationGrant);

        if ((dynamicScopes.size() > 0) && externalDynamicScopeService.isEnabled()) {
            final UnmodifiableAuthorizationGrant unmodifiableAuthorizationGrant = new UnmodifiableAuthorizationGrant(authorizationGrant);
            DynamicScopeExternalContext dynamicScopeContext = new DynamicScopeExternalContext(dynamicScopes, jwr, unmodifiableAuthorizationGrant);
            externalDynamicScopeService.executeExternalUpdateMethods(dynamicScopeContext);
        }

        processCiba(jwr, authorizationGrant, refreshToken);

        if (postProcessing != null) {
        	postProcessing.apply(jwr);
        }
    }

    /**
     * Filters some claims from id_token based on if access_token is issued or not.
     * openid-connect-core-1_0.html Section 5.4
     * @param jwr Json object that contains all claims used in the id_token.
     * @param accessToken Access token issued for this authorization.
     * @param authorizationCode Code issued for this authorization.
     */
    private void filterClaimsBasedOnAccessToken(JsonWebResponse jwr, AccessToken accessToken, AuthorizationCode authorizationCode) {
        if ((accessToken != null || authorizationCode != null) && appConfiguration.getIdTokenFilterClaimsBasedOnAccessToken()) {
            JwtClaims claims = jwr.getClaims();
            claims.removeClaim(JwtClaimName.PROFILE);
            claims.removeClaim(JwtClaimName.EMAIL);
            claims.removeClaim(JwtClaimName.ADDRESS);
            claims.removeClaim(JwtClaimName.PHONE_NUMBER);
        }
    }

    /**
     * Process requested claims in the authorization request.
     * @param requestedClaims Json containing all claims listed in authz request.
     * @param jwr Json that contains all claims that should go in id_token.
     * @param user Authenticated user.
     */
    private void setClaimsFromRequestedClaims(String requestedClaims, JsonWebResponse jwr, User user)
            throws InvalidClaimException {
        if (requestedClaims != null) {
            JSONObject claimsObj = new JSONObject(requestedClaims);
            if (claimsObj.has("id_token")) {
                JSONObject idTokenObj = claimsObj.getJSONObject("id_token");
                for (Iterator<String> it = idTokenObj.keys(); it.hasNext(); ) {
                    String claimName = it.next();
                    GluuAttribute gluuAttribute = attributeService.getByClaimName(claimName);

                    if (gluuAttribute != null) {
                        String ldapClaimName = gluuAttribute.getName();

                        Object attribute = user.getAttribute(ldapClaimName, false, gluuAttribute.getOxMultiValuedAttribute());

                        if (attribute instanceof List) {
                            jwr.getClaims().setClaim(claimName, (List) attribute);
                        } else if (attribute instanceof Boolean) {
                            jwr.getClaims().setClaim(claimName, (Boolean) attribute);
                        } else if (attribute instanceof Date) {
                            jwr.getClaims().setClaim(claimName, ((Date) attribute).getTime());
                        } else {
                            jwr.setClaim(claimName, (String) attribute);
                        }
                    }
                }
            }
        }
    }

    private void processCiba(JsonWebResponse jwr, IAuthorizationGrant authorizationGrant, RefreshToken refreshToken) {
        if (!(authorizationGrant instanceof CIBAGrant)) {
            return;
        }

        String refreshTokenHash = AbstractToken.getHash(refreshToken.getCode(), null);
        jwr.setClaim(JwtClaimName.REFRESH_TOKEN_HASH, refreshTokenHash);

        CIBAGrant cibaGrant = (CIBAGrant) authorizationGrant;
        jwr.setClaim(JwtClaimName.AUTH_REQ_ID, cibaGrant.getAuthReqId());
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
                jwr.getClaims().setClaimFromJsonObject(claim.getName(), attribute);
            }
        }
    }

    public JsonWebResponse createJwr(
            IAuthorizationGrant grant, String nonce,
            AuthorizationCode authorizationCode, AccessToken accessToken, RefreshToken refreshToken,
            String state, Set<String> scopes, boolean includeIdTokenClaims, Function<JsonWebResponse,
            Void> preProcessing, Function<JsonWebResponse, Void> postProcessing, String claims) throws Exception {

        final Client client = grant.getClient();

        JsonWebResponse jwr = jwrService.createJwr(client);
        fillClaims(jwr, grant, nonce, authorizationCode, accessToken, refreshToken, state, scopes,
                includeIdTokenClaims, preProcessing, postProcessing, claims);
        if (log.isTraceEnabled())
            log.trace("Created claims for id_token, claims: " + jwr.getClaims().toJsonString());
        return jwrService.encode(jwr, client);
    }

    private boolean validateRequesteClaim(GluuAttribute gluuAttribute, String[] clientAllowedClaims, Collection<String> scopes) {
        if (gluuAttribute == null) {
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

    public Map<String, Object> getClaims(User user, Scope scope) throws InvalidClaimException, ParseException {
        Map<String, Object> claims = new HashMap<>();

        if (scope == null || scope.getClaims() == null) {
            return claims;
        }

        for (String claimDn : scope.getClaims()) {
            GluuAttribute gluuAttribute = attributeService.getAttributeByDn(claimDn);

            String claimName = gluuAttribute.getClaimName();
            String ldapName = gluuAttribute.getName();
            Object attribute = null;

            if (StringUtils.isNotBlank(claimName) && StringUtils.isNotBlank(ldapName)) {
                if (ldapName.equals("uid")) {
                    attribute = user.getUserId();
                } else if (ldapName.equals("updatedAt")) {
                    attribute = user.getUpdatedAt();
                } if (AttributeDataType.BOOLEAN.equals(gluuAttribute.getDataType())) {
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
                        List<String> values = new ArrayList<>();
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

        return claims;
    }
}