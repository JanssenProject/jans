/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import com.google.common.base.Function;
import org.xdi.oxauth.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.model.ldap.TokenLdap;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.token.JsonWebResponse;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Gives ability to use authorization grant in read-only mode.
 *
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version March 14, 2019
 */

public class UnmodifiableAuthorizationGrant implements IAuthorizationGrant {

    private final IAuthorizationGrant grant;

    public UnmodifiableAuthorizationGrant(IAuthorizationGrant grant) {
        this.grant = grant;
    }

    @Override
    public String getGrantId() {
        return grant.getGrantId();
    }

    @Override
    public void setGrantId(String p_grantId) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public AuthorizationCode getAuthorizationCode() {
        return grant.getAuthorizationCode();
    }

    @Override
    public void setAuthorizationCode(AuthorizationCode authorizationCode) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public String getNonce() {
        return grant.getNonce();
    }

    @Override
    public void setNonce(String nonce) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public AccessToken createAccessToken(String certAsPem) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public RefreshToken createRefreshToken() {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public IdToken createIdToken(String nonce, AuthorizationCode authorizationCode, AccessToken accessToken, String state,
                                 AuthorizationGrant authorizationGrant, boolean includeIdTokenClaims, Function<JsonWebResponse, Void> preProcessing) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
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
    public boolean isValid() {
        return grant.isValid();
    }

    @Override
    public void revokeAllTokens() {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public void checkExpiredTokens() {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public String checkScopesPolicy(String scope) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
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
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
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
    public void setJwtAuthorizationRequest(JwtAuthorizationRequest p_jwtAuthorizationRequest) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public Date getAuthenticationTime() {
        return grant.getAuthenticationTime();
    }

    @Override
    public TokenLdap getTokenLdap() {
        return grant.getTokenLdap();
    }

    @Override
    public void setTokenLdap(TokenLdap p_tokenLdap) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public void setLongLivedAccessToken(AccessToken longLivedAccessToken) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public void setIdToken(IdToken idToken) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public void setScopes(Collection<String> scopes) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public void setAccessTokens(List<AccessToken> accessTokens) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public String getAcrValues() {
        return grant.getAcrValues();
    }

    @Override
    public void setAcrValues(String authMode) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public String getSessionDn() {
        return grant.getSessionDn();
    }

    @Override
    public void setSessionDn(String sessionDn) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }
}
