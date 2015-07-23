/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.xdi.oxauth.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.model.authorize.ScopeChecker;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.federation.FederationTrust;
import org.xdi.oxauth.model.federation.FederationTrustStatus;
import org.xdi.oxauth.model.ldap.TokenLdap;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.service.FederationDataService;
import org.xdi.oxauth.service.ScopeService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version 0.9, 08/14/2014
 */

public abstract class AbstractAuthorizationGrant implements IAuthorizationGrant {

    private static final Logger LOGGER = Logger.getLogger(AbstractAuthorizationGrant.class);

    private final User user;
    private final AuthorizationGrantType authorizationGrantType;
    private final Client client;
    private final Set<String> scopes;

    private String grantId;
    private JwtAuthorizationRequest jwtAuthorizationRequest;
    private Date authenticationTime;
    private TokenLdap tokenLdap;
    private AccessToken longLivedAccessToken;
    private IdToken idToken;
    private AuthorizationCode authorizationCode;
    private String nonce;

    private String acrValues;

    protected final ConcurrentMap<String, AccessToken> accessTokens = new ConcurrentHashMap<String, AccessToken>();
    protected final ConcurrentMap<String, RefreshToken> refreshTokens = new ConcurrentHashMap<String, RefreshToken>();

    protected AbstractAuthorizationGrant(User user, AuthorizationGrantType authorizationGrantType, Client client,
                                         Date authenticationTime) {
        this.authenticationTime = authenticationTime != null ? new Date(authenticationTime.getTime()) : null;
        this.user = user;
        this.authorizationGrantType = authorizationGrantType;
        this.client = client;
        this.scopes = new CopyOnWriteArraySet<String>();
        this.grantId = UUID.randomUUID().toString();
    }

    @Override
    public synchronized String getGrantId() {
        return grantId;
    }

    @Override
    public synchronized void setGrantId(String p_grantId) {
        grantId = p_grantId;
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

    @Override
    public String getNonce() {
        return nonce;
    }

    @Override
    public void setNonce(String nonce) {
        this.nonce = nonce;
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
        return new ArrayList<AccessToken>(accessTokens.values());
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
    public TokenLdap getTokenLdap() {
        return tokenLdap;
    }

    @Override
    public void setTokenLdap(TokenLdap p_tokenLdap) {
        this.tokenLdap = p_tokenLdap;
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

    /**
     * Checks the scopes policy configured according to the type of the
     * authorization grant to limit the issued token scopes.
     *
     * @param scope A space-delimited list of values in which the order of
     *              values does not matter.
     * @return A space-delimited list of scopes
     */
    @Override
    public String checkScopesPolicy(String requestedScopes) {
    	this.scopes.clear();

    	Set<String> grantedScopes = ScopeChecker.instance().checkScopesPolicy(client, requestedScopes);
    	this.scopes.addAll(grantedScopes);
    	
        final StringBuilder grantedScopesSb = new StringBuilder();
        for (String scope : scopes) {
            grantedScopesSb.append(" ").append(scope);

        }

        final String grantedScopesSt = grantedScopesSb.toString().trim();

    	return grantedScopesSt;
    }

    @Override
    public AccessToken createAccessToken() {
        int lifetime = ConfigurationFactory.instance().getConfiguration().getShortLivedAccessTokenLifetime();
        AccessToken accessToken = new AccessToken(lifetime);

        accessToken.setAuthMode(getAcrValues());

        return accessToken;
    }

    @Override
    public AccessToken createLongLivedAccessToken() {
        int lifetime = ConfigurationFactory.instance().getConfiguration().getLongLivedAccessTokenLifetime();
        AccessToken accessToken = new AccessToken(lifetime);

        accessToken.setAuthMode(getAcrValues());

        return accessToken;
    }

    @Override
    public RefreshToken createRefreshToken() {
        int lifetime = ConfigurationFactory.instance().getConfiguration().getRefreshTokenLifetime();
        RefreshToken refreshToken = new RefreshToken(lifetime);

        refreshToken.setAuthMode(getAcrValues());

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
     * Returns the {@link org.xdi.oxauth.model.registration.Client}. An
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
        return authenticationTime != null ? new Date(authenticationTime.getTime()) : null;
    }

    public void setAuthenticationTime(Date authenticationTime) {
        this.authenticationTime = authenticationTime != null ? new Date(authenticationTime.getTime()) : null;
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
    public void setJwtAuthorizationRequest(JwtAuthorizationRequest p_jwtAuthorizationRequest) {
        jwtAuthorizationRequest = p_jwtAuthorizationRequest;
    }

    @Override
    public void setAccessTokens(List<AccessToken> accessTokens) {
        put(this.accessTokens, accessTokens);
    }

    private static <T extends AbstractToken> void put(ConcurrentMap<String, T> p_map, List<T> p_list) {
        p_map.clear();
        if (p_list != null && !p_list.isEmpty()) {
            for (T t : p_list) {
                p_map.put(t.getCode(), t);
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
        return new ArrayList<RefreshToken>(refreshTokens.values());
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
     * @return The refresh token instance or
     *         <code>null</code> if not found.
     */
    @Override
    public RefreshToken getRefreshToken(String refreshTokenCode) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Looking for the refresh token: " + refreshTokenCode
                    + " for an authorization grant of type: " + getAuthorizationGrantType());
        }

        return refreshTokens.get(refreshTokenCode);
    }

    /**
     * Gets the access token instance from the id token list or the access token
     * list given its code.
     *
     * @param tokenCode The code of the access token.
     * @return The access token instance or
     *         <code>null</code> if not found.
     */
    @Override
    public AbstractToken getAccessToken(String tokenCode) {
        final IdToken idToken = getIdToken();
        if (idToken != null) {
            if (idToken.getCode().equals(tokenCode)) {
                return idToken;
            }
        }

        final AccessToken longLivedAccessToken = getLongLivedAccessToken();
        if (longLivedAccessToken != null) {
            if (longLivedAccessToken.getCode().equals(tokenCode)) {
                return longLivedAccessToken;
            }
        }

        return accessTokens.get(tokenCode);
    }
}