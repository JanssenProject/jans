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
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.authorize.CodeVerifier;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.exception.InvalidClaimException;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.jwt.JwtSubClaimObject;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.authorize.ws.rs.AuthzRequest;
import io.jans.as.server.model.authorize.Claim;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.as.server.model.common.*;
import io.jans.as.server.service.AcrService;
import io.jans.as.server.service.ScopeService;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.date.DateFormatterService;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.service.external.ExternalAuthorizationChallengeService;
import io.jans.as.server.service.external.ExternalDynamicScopeService;
import io.jans.as.server.service.external.ExternalUpdateTokenService;
import io.jans.as.server.service.external.context.DynamicScopeExternalContext;
import io.jans.as.server.service.external.context.ExternalUpdateTokenContext;
import io.jans.as.server.service.token.StatusListService;
import io.jans.model.JansAttribute;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.auth.PersonAuthenticationType;
import jakarta.ejb.Stateless;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.*;

import static io.jans.as.model.common.ScopeType.DYNAMIC;
import static io.jans.as.server.token.ws.rs.TokenExchangeService.DEVICE_SECRET;

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
    private ExternalUpdateTokenService externalUpdateTokenService;

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

    @Inject
    private DateFormatterService dateFormatterService;

    @Inject
    private StatusListService statusListService;

    @Inject
    private ExternalAuthorizationChallengeService externalAuthorizationChallengeService;

    private void setAmrClaim(JsonWebResponse jwt, String acrValues, Client client) {
        List<String> amrList = Lists.newArrayList();

        CustomScriptConfiguration script = externalAuthenticationService.getCustomScriptConfigurationByName(acrValues);
        if (script != null) {
            amrList.add(Integer.toString(script.getLevel()));

            PersonAuthenticationType externalAuthenticator = (PersonAuthenticationType) script.getExternalType();
            int apiVersion = externalAuthenticator.getApiVersion();

            if (apiVersion > 3) {
                Map<String, String> authenticationMethodClaimsOrNull = externalAuthenticator.getAuthenticationMethodClaims(script.getConfigurationAttributes());
                addToAmrList(amrList, authenticationMethodClaimsOrNull);
            }
        } else {
            AuthzRequest authzRequest = new AuthzRequest();
            authzRequest.setAcrValues(acrValues);
            authzRequest.setClientId(client.getClientId());

            ExecutionContext executionContext = new ExecutionContext();
            executionContext.setAuthzRequest(authzRequest);
            executionContext.setClient(client);

            final Map<String, String> amrMap = externalAuthorizationChallengeService.getAuthenticationMethodClaims(executionContext);
            addToAmrList(amrList, amrMap);
        }

        jwt.getClaims().setClaim(JwtClaimName.AUTHENTICATION_METHOD_REFERENCES, amrList);
    }

    private void addToAmrList(List<String> amrList, Map<String, String> claims) {
        if (claims != null) {
            for (Map.Entry<String, String> entry : claims.entrySet()) {
                amrList.add(entry.getKey() + ":" + entry.getValue());
            }
        }
    }

    private void fillClaims(JsonWebResponse jwr,
                            IAuthorizationGrant authorizationGrant,
                            AuthorizationCode authorizationCode, AccessToken accessToken, RefreshToken refreshToken,
                            ExecutionContext executionContext) throws Exception {

        final Client client = authorizationGrant.getClient();
        jwr.getClaims().setIssuer(appConfiguration.getIssuer());
        Audience.setAudience(jwr.getClaims(), client);

        int lifeTime = appConfiguration.getIdTokenLifetime();
        if (client.getAttributes().getIdTokenLifetime() != null && client.getAttributes().getIdTokenLifetime() > 0) {
            lifeTime = client.getAttributes().getIdTokenLifetime();
            log.trace("Override id token lifetime with value from client: {}", client.getClientId());
        }
        int lifetimeFromScript = externalUpdateTokenService.getIdTokenLifetimeInSeconds(ExternalUpdateTokenContext.of(executionContext));
        if (lifetimeFromScript > 0) {
            lifeTime = lifetimeFromScript;
            log.trace("Override id token lifetime with value from script: {}", lifetimeFromScript);
        }

        Calendar calendar = Calendar.getInstance();
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.SECOND, lifeTime);
        Date expiration = calendar.getTime();

        jwr.getClaims().setExpirationTime(expiration);
        jwr.getClaims().setIat(issuedAt);
        jwr.getClaims().setNbf(issuedAt);
        jwr.setClaim("jti", executionContext.getTokenReferenceId()); // provided uniqueness of id_token for same RP requests, oxauth: 1493

        if (executionContext.getPreProcessing() != null) {
            executionContext.getPreProcessing().apply(jwr);
        }
        final SessionId session = sessionIdService.getSessionByDn(authorizationGrant.getSessionDn(), true);
        if (session != null) {
            jwr.setClaim("sid", session.getOutsideSid());
        }

        statusListService.addStatusClaimWithIndex(jwr, executionContext);

        addTokenExchangeClaims(jwr, executionContext, session);

        String acrValues = authorizationGrant.getAcrValues();
        acrValues = AcrService.removeParametersFromAgamaAcr(acrValues);
        if (acrValues != null) {
            jwr.setClaim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, acrValues);
            setAmrClaim(jwr, acrValues, client);
        }
        String nonce = executionContext.getNonce();
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
        String state = executionContext.getState();
        if (Strings.isNotBlank(state)) {
            String stateHash = AbstractToken.getHash(state, jwr.getHeader().getSignatureAlgorithm());
            jwr.setClaim(JwtClaimName.STATE_HASH, stateHash);
        }
        if (authorizationGrant.getGrantType() != null) {
            jwr.setClaim("grant", authorizationGrant.getGrantType().getValue());
        }
        jwr.setClaim(JwtClaimName.JANS_OPENID_CONNECT_VERSION, appConfiguration.getJansOpenIdConnectVersion());

        User user = authorizationGrant.getUser();
        List<Scope> dynamicScopes = new ArrayList<>();
        if (executionContext.isIncludeIdTokenClaims() && client.isIncludeClaimsInIdToken()) {
            for (String scopeName : executionContext.getScopes()) {
                Scope scope = scopeService.getScopeById(scopeName);
                if (scope == null) {
                    continue;
                }

                if (DYNAMIC == scope.getScopeType()) {
                    dynamicScopes.add(scope);
                    continue;
                }

                Map<String, Object> claims = scopeService.getClaims(user, scope);

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
                            Serializable formattedValue = dateFormatterService.formatClaim((Date) value, key);
                            jwr.getClaims().setClaimObject(key, formattedValue, true);
                        } else {
                            jwr.setClaim(key, (String) value);
                        }
                    }
                }

                jwr.getClaims().setSubjectIdentifier(authorizationGrant.getUser().getAttribute("inum"));
            }
        }

        setClaimsFromJwtAuthorizationRequest(jwr, authorizationGrant, executionContext.getScopes());
        setClaimsFromRequestedClaims(executionContext.getClaimsAsString(), jwr, user);
        filterClaimsBasedOnAccessToken(jwr, accessToken, authorizationCode);
        jwrService.setSubjectIdentifier(jwr, authorizationGrant);

        if ((dynamicScopes.size() > 0) && externalDynamicScopeService.isEnabled()) {
            final UnmodifiableAuthorizationGrant unmodifiableAuthorizationGrant = new UnmodifiableAuthorizationGrant(authorizationGrant);
            DynamicScopeExternalContext dynamicScopeContext = new DynamicScopeExternalContext(dynamicScopes, jwr, unmodifiableAuthorizationGrant);
            externalDynamicScopeService.executeExternalUpdateMethods(dynamicScopeContext);
        }

        processCiba(jwr, authorizationGrant, refreshToken);

        if (executionContext.getPostProcessor() != null) {
            executionContext.getPostProcessor().apply(jwr);
        }
    }

    private void addTokenExchangeClaims(JsonWebResponse jwr, ExecutionContext executionContext, SessionId sessionId) {
        if (sessionId == null) { // unable to find session
            return;
        }

        String deviceSecret = executionContext.getDeviceSecret();
        if (StringUtils.isBlank(deviceSecret)) {
            deviceSecret = executionContext.getHttpRequest().getParameter(DEVICE_SECRET);
        }

        if (StringUtils.isNotBlank(deviceSecret) && sessionId.getDeviceSecrets().contains(deviceSecret)) {
            jwr.setClaim("ds_hash", CodeVerifier.s256(deviceSecret));
        }
    }

    /**
     * Filters some claims from id_token based on if access_token is issued or not.
     * openid-connect-core-1_0.html Section 5.4
     *
     * @param jwr               Json object that contains all claims used in the id_token.
     * @param accessToken       Access token issued for this authorization.
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
     *
     * @param requestedClaims Json containing all claims listed in authz request.
     * @param jwr             Json that contains all claims that should go in id_token.
     * @param user            Authenticated user.
     */
    private void setClaimsFromRequestedClaims(String requestedClaims, JsonWebResponse jwr, User user)
            throws InvalidClaimException {
        if (requestedClaims != null) {
            JSONObject claimsObj = new JSONObject(requestedClaims);
            if (claimsObj.has("id_token")) {
                JSONObject idTokenObj = claimsObj.getJSONObject("id_token");
                for (Iterator<String> it = idTokenObj.keys(); it.hasNext(); ) {
                    String claimName = it.next();
                    JansAttribute jansAttribute = attributeService.getByClaimName(claimName);

                    if (jansAttribute != null) {
                        String ldapClaimName = jansAttribute.getName();

                        Object attribute = user.getAttribute(ldapClaimName, false, jansAttribute.getOxMultiValuedAttribute());

                        if (attribute instanceof List) {
                            jwr.getClaims().setClaim(claimName, (List) attribute);
                        } else if (attribute instanceof Boolean) {
                            jwr.getClaims().setClaim(claimName, (Boolean) attribute);
                        } else if (attribute instanceof Date) {
                            jwr.getClaims().setClaim(claimName, ((Date) attribute).getTime() / 1000);
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
            JansAttribute jansAttribute = attributeService.getByClaimName(claim.getName());

            if (jansAttribute == null) {
                continue;
            }

            Client client = authorizationGrant.getClient();

            if (validateRequesteClaim(jansAttribute, client.getClaims(), scopes)) {
                String ldapClaimName = jansAttribute.getName();
                Object attribute = authorizationGrant.getUser().getAttribute(ldapClaimName, optional, jansAttribute.getOxMultiValuedAttribute());
                jwr.getClaims().setClaimFromJsonObject(claim.getName(), attribute);
            }
        }
    }

    public JsonWebResponse createJwr(
            IAuthorizationGrant grant, AuthorizationCode authorizationCode, AccessToken accessToken, RefreshToken refreshToken,
            ExecutionContext executionContext) throws Exception {

        final Client client = grant.getClient();

        JsonWebResponse jwr = jwrService.createJwr(client);
        fillClaims(jwr, grant, authorizationCode, accessToken, refreshToken, executionContext);
        if (log.isTraceEnabled())
            log.trace("Created claims for id_token, claims: {}", jwr.getClaims().toJsonString());
        return jwrService.encode(jwr, client);
    }

    private boolean validateRequesteClaim(JansAttribute jansAttribute, String[] clientAllowedClaims, Collection<String> scopes) {
        if (jansAttribute == null) {
            return false;
        }

        if (clientAllowedClaims != null) {
            for (String clientAllowedClaim : clientAllowedClaims) {
                if (jansAttribute.getDn().equals(clientAllowedClaim)) {
                    return true;
                }
            }
        }

        for (String scopeName : scopes) {
            Scope scope = scopeService.getScopeById(scopeName);

            if (scope != null && scope.getClaims() != null) {
                for (String claimDn : scope.getClaims()) {
                    if (jansAttribute.getDisplayName().equals(attributeService.getAttributeByDn(claimDn).getDisplayName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}