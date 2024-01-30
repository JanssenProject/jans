/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.GrantType;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.model.token.TokenEntity;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */

public interface IAuthorizationGrant {

    GrantType getGrantType();

    String getGrantId();

    void setGrantId(String grantId);

    AuthorizationCode getAuthorizationCode();

    void setAuthorizationCode(AuthorizationCode authorizationCode);

    String getNonce();

    void setNonce(String nonce);

    String getSub();

    AccessToken createAccessToken(ExecutionContext executionContext);

    RefreshToken createRefreshToken(ExecutionContext executionContext);

    RefreshToken createRefreshToken(ExecutionContext executionContext, int lifetime);

    IdToken createIdToken(
            String nonce, AuthorizationCode authorizationCode, AccessToken accessToken, RefreshToken refreshToken,
            String state, ExecutionContext executionContext);

    RefreshToken getRefreshToken(String refreshTokenCode);

    AbstractToken getAccessToken(String tokenCode);

    void revokeAllTokens();

    void checkExpiredTokens();

    String checkScopesPolicy(String scope);

    User getUser();

    String getUserId();

    String getUserDn();

    AuthorizationGrantType getAuthorizationGrantType();

    String getClientId();

    Client getClient();

    String getClientDn();

    List<AccessToken> getAccessTokens();

    Set<String> getScopes();

    Set<String> getRefreshTokensCodes();

    Set<String> getAccessTokensCodes();

    List<RefreshToken> getRefreshTokens();

    void setRefreshTokens(List<RefreshToken> refreshTokens);

    AccessToken getLongLivedAccessToken();

    IdToken getIdToken();

    JwtAuthorizationRequest getJwtAuthorizationRequest();

    void setJwtAuthorizationRequest(JwtAuthorizationRequest jwtAuthorizationRequest);

    Date getAuthenticationTime();

    TokenEntity getTokenEntity();

    void setTokenEntity(TokenEntity tokenEntity);

    void setLongLivedAccessToken(AccessToken longLivedAccessToken);

    void setIdToken(IdToken idToken);

    void setScopes(Collection<String> scopes);

    void setAccessTokens(List<AccessToken> accessTokens);

    void setTxTokens(List<TxToken> txTokens);

    String getAcrValues();

    void setAcrValues(String authMode);

    String getSessionDn();

    void setSessionDn(String sessionDn);

    /**
     * Saves changes asynchronously
     */
    void save();
}