/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.TokenType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.CertUtils;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.model.ldap.TokenEntity;
import io.jans.as.server.service.KeyGeneratorTimer;
import io.jans.as.server.service.external.ExternalUpdateTokenService;
import io.jans.as.server.service.external.context.ExternalUpdateTokenContext;
import io.jans.as.server.util.TokenHashUtil;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version September 30, 2021
 */

public abstract class AbstractAuthorizationGrant implements IAuthorizationGrant {

    private static final Logger log = LoggerFactory.getLogger(AbstractAuthorizationGrant.class);

    @Inject
    protected AppConfiguration appConfiguration;

    @Inject
    private ExternalUpdateTokenService externalUpdateTokenService;

    @Inject
    protected ScopeChecker scopeChecker;

    @Inject
    private KeyGeneratorTimer keyGeneratorTimer;

    private User user;
    private AuthorizationGrantType authorizationGrantType;
    private Client client;
    private Set<String> scopes;

    private String grantId;
    private JwtAuthorizationRequest jwtAuthorizationRequest;
    private Date authenticationTime;
    private TokenEntity tokenEntity;
    private AccessToken longLivedAccessToken;
    private IdToken idToken;
    private AuthorizationCode authorizationCode;
    private String tokenBindingHash;
    private String x5cs256;
    private String nonce;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String claims;

    private String acrValues;
    private String sessionDn;

    protected final ConcurrentMap<String, AccessToken> accessTokens = new ConcurrentHashMap<>();
    protected final ConcurrentMap<String, RefreshToken> refreshTokens = new ConcurrentHashMap<>();

    protected AbstractAuthorizationGrant() {
    }

    protected AbstractAuthorizationGrant(User user, AuthorizationGrantType authorizationGrantType, Client client,
                                         Date authenticationTime) {
        init(user, authorizationGrantType, client, authenticationTime);
    }

    protected void init(User user, AuthorizationGrantType authorizationGrantType, Client client,
                        Date authenticationTime) {
        this.authenticationTime = authenticationTime != null ? new Date(authenticationTime.getTime()) : null;
        this.user = user;
        this.authorizationGrantType = authorizationGrantType;
        this.client = client;
        this.scopes = new CopyOnWriteArraySet<>();
        this.grantId = UUID.randomUUID().toString();
    }

    @Override
    public synchronized String getGrantId() {
        return grantId;
    }

    @Override
    public synchronized void setGrantId(String grantId) {
        this.grantId = grantId;
    }

    /**
     * Returns the {@link AuthorizationCode}.
     *
     * @return The authorization code.
     */
    @Override
    public AuthorizationCode getAuthorizationCode() {
        return authorizationCode;
    }

    /**
     * Sets the {@link AuthorizationCode}.
     *
     * @param authorizationCode The authorization code.
     */
    @Override
    public void setAuthorizationCode(AuthorizationCode authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public String getTokenBindingHash() {
        return tokenBindingHash;
    }

    public void setTokenBindingHash(String tokenBindingHash) {
        this.tokenBindingHash = tokenBindingHash;
    }

    public String getX5cs256() {
        return x5cs256;
    }

    public void setX5cs256(String x5cs256) {
        this.x5cs256 = x5cs256;
    }

    @Override
    public String getNonce() {
        return nonce;
    }

    @Override
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public void setCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }

    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

    public void setCodeChallengeMethod(String codeChallengeMethod) {
        this.codeChallengeMethod = codeChallengeMethod;
    }

    public String getClaims() {
        return claims;
    }

    public void setClaims(String claims) {
        this.claims = claims;
    }

    /**
     * Returns a list with all the issued refresh tokens codes.
     *
     * @return List with all the issued refresh tokens codes.
     */
    @Override
    public Set<String> getRefreshTokensCodes() {
        return refreshTokens.keySet();
    }

    /**
     * Returns a list with all the issued access tokens codes.
     *
     * @return List with all the issued access tokens codes.
     */
    @Override
    public Set<String> getAccessTokensCodes() {
        return accessTokens.keySet();
    }

    /**
     * Returns a list with all the issued access tokens.
     *
     * @return List with all the issued access tokens.
     */
    @Override
    public List<AccessToken> getAccessTokens() {
        return new ArrayList<>(accessTokens.values());
    }

    @Override
    public void setScopes(Collection<String> scopes) {
        this.scopes.clear();
        this.scopes.addAll(scopes);
    }

    @Override
    public AccessToken getLongLivedAccessToken() {
        return longLivedAccessToken;
    }

    @Override
    public void setLongLivedAccessToken(AccessToken longLivedAccessToken) {
        this.longLivedAccessToken = longLivedAccessToken;
    }

    @Override
    public IdToken getIdToken() {
        return idToken;
    }

    @Override
    public void setIdToken(IdToken idToken) {
        this.idToken = idToken;
    }

    @Override
    public TokenEntity getTokenEntity() {
        return tokenEntity;
    }

    @Override
    public void setTokenEntity(TokenEntity tokenEntity) {
        this.tokenEntity = tokenEntity;
    }

    /**
     * Returns the resource owner's.
     *
     * @return The resource owner's.
     */
    @Override
    public User getUser() {
        return user;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    public String getSessionDn() {
        return sessionDn;
    }

    public void setSessionDn(String sessionDn) {
        this.sessionDn = sessionDn;
    }

    /**
     * Checks the scopes policy configured according to the type of the
     * authorization grant to limit the issued token scopes.
     *
     * @param requestedScopes A space-delimited list of values in which the order of values
     *                        does not matter.
     * @return A space-delimited list of scopes
     */
    @Override
    public String checkScopesPolicy(String requestedScopes) {
        this.scopes.clear();

        Set<String> grantedScopes = scopeChecker.checkScopesPolicy(client, requestedScopes);
        this.scopes.addAll(grantedScopes);

        final StringBuilder grantedScopesSb = new StringBuilder();
        for (String scope : scopes) {
            grantedScopesSb.append(" ").append(scope);
        }

        return grantedScopesSb.toString().trim();
    }

    public int getAccessTokenLifetimeInSeconds(ExecutionContext executionContext) {
        int lifetime = appConfiguration.getAccessTokenLifetime();
        // Jans Auth #830 Client-specific access token expiration
        if (client != null && client.getAccessTokenLifetime() != null && client.getAccessTokenLifetime() > 0) {
            lifetime = client.getAccessTokenLifetime();
        }

        int lifetimeFromScript = externalUpdateTokenService.getAccessTokenLifetimeInSeconds(ExternalUpdateTokenContext.of(executionContext));
        if (lifetimeFromScript > 0) {
            lifetime = lifetimeFromScript;
            log.trace("Override access token lifetime with value from script: {}", lifetimeFromScript);
        }

        if (client != null && client.isAccessTokenAsJwt() && appConfiguration.getKeyRegenerationEnabled()) {
            int intervalInSeconds = appConfiguration.getKeyRegenerationInterval() * 3600;
            int timePassedInSeconds = (int) ((System.currentTimeMillis() - keyGeneratorTimer.getLastFinishedTime()) / 1000);
            final int recalculcatedLifetime = intervalInSeconds - timePassedInSeconds;
            if (recalculcatedLifetime > 0) {
                log.trace("Override access token lifetime based on key lifetime: {}", recalculcatedLifetime);
                lifetime = recalculcatedLifetime;
            }
        }
        return lifetime;
    }

    @Override
    public AccessToken createAccessToken(ExecutionContext executionContext) {
        AccessToken accessToken = new AccessToken(getAccessTokenLifetimeInSeconds(executionContext));

        accessToken.setSessionDn(getSessionDn());
        accessToken.setX5ts256(CertUtils.confirmationMethodHashS256(executionContext.getCertAsPem()));

        final String dpop = executionContext.getDpop();
        if (StringUtils.isNoneBlank(dpop)) {
            accessToken.setDpop(dpop);
            accessToken.setTokenType(TokenType.DPOP);
        }

        return accessToken;
    }

    @Override
    public RefreshToken createRefreshToken(ExecutionContext context) {
        int lifetime = appConfiguration.getRefreshTokenLifetime();
        if (client.getRefreshTokenLifetime() != null && client.getRefreshTokenLifetime() > 0) {
            lifetime = client.getRefreshTokenLifetime();
        }

        RefreshToken refreshToken = new RefreshToken(lifetime);

        refreshToken.setSessionDn(getSessionDn());
        refreshToken.setDpop(context.getDpop());
        return refreshToken;
    }

    @Override
    public RefreshToken createRefreshToken(ExecutionContext context, int lifetime) {
        RefreshToken refreshToken = new RefreshToken(lifetime);

        refreshToken.setSessionDn(getSessionDn());
        refreshToken.setDpop(context.getDpop());

        return refreshToken;
    }

    @Override
    public String getUserId() {
        if (user == null) {
            return null;
        }

        return user.getUserId();
    }

    @Override
    public String getUserDn() {
        if (user == null) {
            return null;
        }

        return user.getDn();
    }

    /**
     * Returns the {@link AuthorizationGrantType}.
     *
     * @return The authorization grant type.
     */
    @Override
    public AuthorizationGrantType getAuthorizationGrantType() {
        return authorizationGrantType;
    }

    /**
     * Returns the {@link Client}. An
     * application making protected resource requests on behalf of the resource
     * owner and with its authorization.
     *
     * @return The client.
     */
    @Override
    public Client getClient() {
        return client;
    }

    @Override
    public String getClientId() {
        if (client == null) {
            return null;
        }

        return client.getClientId();
    }

    @Override
    public String getClientDn() {
        if (client == null) {
            return null;
        }

        return client.getDn();
    }

    @Override
    public Date getAuthenticationTime() {
        return authenticationTime;
    }

    public void setAuthenticationTime(Date authenticationTime) {
        this.authenticationTime = authenticationTime;
    }

    /**
     * Returns a list of the scopes granted to the client.
     *
     * @return List of the scopes granted to the client.
     */
    @Override
    public Set<String> getScopes() {
        return scopes;
    }

    @Override
    public JwtAuthorizationRequest getJwtAuthorizationRequest() {
        return jwtAuthorizationRequest;
    }

    @Override
    public void setJwtAuthorizationRequest(JwtAuthorizationRequest jwtAuthorizationRequest) {
        this.jwtAuthorizationRequest = jwtAuthorizationRequest;
    }

    @Override
    public void setAccessTokens(List<AccessToken> accessTokens) {
        put(this.accessTokens, accessTokens);
    }

    private static <T extends AbstractToken> void put(ConcurrentMap<String, T> map, List<T> list) {
        map.clear();
        if (list != null && !list.isEmpty()) {
            for (T t : list) {
                map.put(t.getCode(), t);
            }
        }
    }

    /**
     * Returns a list with all the issued refresh tokens.
     *
     * @return List with all the issued refresh tokens.
     */
    @Override
    public List<RefreshToken> getRefreshTokens() {
        return new ArrayList<>(refreshTokens.values());
    }

    @Override
    public void setRefreshTokens(List<RefreshToken> refreshTokens) {
        put(this.refreshTokens, refreshTokens);
    }

    /**
     * Gets the refresh token instance from the refresh token list given its
     * code.
     *
     * @param refreshTokenCode The code of the refresh token.
     * @return The refresh token instance or <code>null</code> if not found.
     */
    @Override
    public RefreshToken getRefreshToken(String refreshTokenCode) {
        if (log.isTraceEnabled()) {
            log.trace("Looking for the refresh token: {} for an authorization grant of type: {}",
                    refreshTokenCode, getAuthorizationGrantType());
        }
        return refreshTokens.get(TokenHashUtil.hash(refreshTokenCode));
    }

    /**
     * Gets the access token instance from the id token list or the access token
     * list given its code.
     *
     * @param tokenCode The code of the access token.
     * @return The access token instance or <code>null</code> if not found.
     */
    @Override
    public AbstractToken getAccessToken(String tokenCode) {

        String hashedTokenCode = TokenHashUtil.hash(tokenCode);

        if (idToken != null && idToken.getCode().equals(hashedTokenCode)) {
            return idToken;
        }

        if (longLivedAccessToken != null && longLivedAccessToken.getCode().equals(hashedTokenCode)) {
            return longLivedAccessToken;
        }

        return accessTokens.get(hashedTokenCode);
    }

    @Override
    public String toString() {
        return "AbstractAuthorizationGrant{" + "user=" + user + ", authorizationCode=" + authorizationCode + ", client="
                + client + ", grantId='" + grantId + '\'' + ", nonce='" + nonce + '\'' + ", acrValues='" + acrValues
                + '\'' + ", sessionDn='" + sessionDn + '\'' + ", codeChallenge='" + codeChallenge + '\''
                + ", codeChallengeMethod='" + codeChallengeMethod + '\'' + ", authenticationTime=" + authenticationTime
                + ", scopes=" + scopes + ", authorizationGrantType=" + authorizationGrantType + ", tokenBindingHash=" + tokenBindingHash
                + ", x5cs256=" + x5cs256 + ", claims=" + claims + '}';
    }
}