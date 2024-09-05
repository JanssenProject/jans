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
 * Gives ability to use authorization grant in read-only mode.
 *
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */

public class UnmodifiableAuthorizationGrant implements IAuthorizationGrant {

    public static final String NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT = "Not allowed for UnmodifiableAuthorizationGrant.";

    private final IAuthorizationGrant grant;

    public UnmodifiableAuthorizationGrant(IAuthorizationGrant grant) {
        this.grant = grant;
    }

    @Override
    public GrantType getGrantType() {
        return GrantType.NONE;
    }

    @Override
    public String getGrantId() {
        return grant.getGrantId();
    }

    @Override
    public void setGrantId(String grantId) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public AuthorizationCode getAuthorizationCode() {
        return grant.getAuthorizationCode();
    }

    @Override
    public void setAuthorizationCode(AuthorizationCode authorizationCode) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public String getNonce() {
        return grant.getNonce();
    }

    @Override
    public void setNonce(String nonce) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public String getSub() {
        return grant.getSub();
    }

    @Override
    public AccessToken createAccessToken(ExecutionContext executionContext) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public RefreshToken createRefreshToken(ExecutionContext executionContext) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public RefreshToken createRefreshToken(ExecutionContext executionContext, int lifetime) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public IdToken createIdToken(
            String nonce, AuthorizationCode authorizationCode, AccessToken accessToken, RefreshToken refreshToken,
            String state, ExecutionContext executionContext) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public RefreshToken getRefreshToken(String refreshTokenCode) {
        return grant.getRefreshToken(refreshTokenCode);
    }

    @Override
    public AbstractToken getAccessToken(String tokenCode) {
        return grant.getAccessToken(tokenCode);
    }

    @Override
    public void revokeAllTokens() {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public void checkExpiredTokens() {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public String checkScopesPolicy(String scope) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public User getUser() {
        return grant.getUser();
    }

    @Override
    public String getUserId() {
        return grant.getUserId();
    }

    @Override
    public String getUserDn() {
        return grant.getUserDn();
    }

    @Override
    public AuthorizationGrantType getAuthorizationGrantType() {
        return grant.getAuthorizationGrantType();
    }

    @Override
    public String getClientId() {
        return grant.getClientId();
    }

    @Override
    public Client getClient() {
        return grant.getClient();
    }

    @Override
    public String getClientDn() {
        return grant.getClientDn();
    }

    @Override
    public List<AccessToken> getAccessTokens() {
        return grant.getAccessTokens();
    }

    @Override
    public Set<String> getScopes() {
        return grant.getScopes();
    }

    @Override
    public Set<String> getRefreshTokensCodes() {
        return grant.getRefreshTokensCodes();
    }

    @Override
    public Set<String> getAccessTokensCodes() {
        return grant.getAccessTokensCodes();
    }

    @Override
    public List<RefreshToken> getRefreshTokens() {
        return grant.getRefreshTokens();
    }

    @Override
    public void setRefreshTokens(List<RefreshToken> refreshTokens) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public AccessToken getLongLivedAccessToken() {
        return grant.getLongLivedAccessToken();
    }

    @Override
    public IdToken getIdToken() {
        return grant.getIdToken();
    }

    @Override
    public JwtAuthorizationRequest getJwtAuthorizationRequest() {
        return grant.getJwtAuthorizationRequest();
    }

    @Override
    public void setJwtAuthorizationRequest(JwtAuthorizationRequest jwtAuthorizationRequest) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public Date getAuthenticationTime() {
        return grant.getAuthenticationTime();
    }

    @Override
    public TokenEntity getTokenEntity() {
        return grant.getTokenEntity();
    }

    @Override
    public void setTokenEntity(TokenEntity token) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public void setLongLivedAccessToken(AccessToken longLivedAccessToken) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public void setIdToken(IdToken idToken) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public void setScopes(Collection<String> scopes) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public void setAccessTokens(List<AccessToken> accessTokens) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public void setTxTokens(List<TxToken> txTokens) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public String getAcrValues() {
        return grant.getAcrValues();
    }

    @Override
    public void setAcrValues(String authMode) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public String getSessionDn() {
        return grant.getSessionDn();
    }

    @Override
    public void setSessionDn(String sessionDn) {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException(NOT_ALLOWED_FOR_UNMODIFIABLE_AUTHORIZATION_GRANT);
    }
}
