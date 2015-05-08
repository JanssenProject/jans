/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import org.xdi.oxauth.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.ldap.TokenLdap;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.util.security.StringEncrypter;

import java.security.SignatureException;
import java.util.*;

/**
 * Gives ability to use authorization grant in read-only mode.
 *
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version 0.9 April 27, 2015
 */

public class UnmodifiableAuthorizationGrant implements IAuthorizationGrant {

    private final IAuthorizationGrant m_grant;

    public UnmodifiableAuthorizationGrant(IAuthorizationGrant p_grant) {
        m_grant = p_grant;
    }

    @Override
    public String getGrantId() {
        return m_grant.getGrantId();
    }

    @Override
    public void setGrantId(String p_grantId) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public AuthorizationCode getAuthorizationCode() {
        return m_grant.getAuthorizationCode();
    }

    @Override
    public void setAuthorizationCode(AuthorizationCode authorizationCode) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public String getNonce() {
        return m_grant.getNonce();
    }

    @Override
    public void setNonce(String nonce) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public AccessToken createAccessToken() {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public AccessToken createLongLivedAccessToken() {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public RefreshToken createRefreshToken() {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public IdToken createIdToken(String nonce, AuthorizationCode authorizationCode, AccessToken accessToken,
                                 Map<String, String> claims, String authMode, String authLevel)
            throws SignatureException, StringEncrypter.EncryptionException, InvalidJwtException, InvalidJweException {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public RefreshToken getRefreshToken(String refreshTokenCode) {
        return m_grant.getRefreshToken(refreshTokenCode);
    }

    @Override
    public AbstractToken getAccessToken(String tokenCode) {
        return m_grant.getAccessToken(tokenCode);
    }

    @Override
    public boolean isValid() {
        return m_grant.isValid();
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
        return m_grant.getUser();
    }

    @Override
    public String getUserId() {
        return m_grant.getUserId();
    }

    @Override
    public String getUserDn() {
        return m_grant.getUserDn();
    }

    @Override
    public AuthorizationGrantType getAuthorizationGrantType() {
        return m_grant.getAuthorizationGrantType();
    }

    @Override
    public String getClientId() {
        return m_grant.getClientId();
    }

    @Override
    public Client getClient() {
        return m_grant.getClient();
    }

    @Override
    public String getClientDn() {
        return m_grant.getClientDn();
    }

    @Override
    public List<AccessToken> getAccessTokens() {
        return m_grant.getAccessTokens();
    }

    @Override
    public Set<String> getScopes() {
        return m_grant.getScopes();
    }

    @Override
    public Set<String> getRefreshTokensCodes() {
        return m_grant.getRefreshTokensCodes();
    }

    @Override
    public Set<String> getAccessTokensCodes() {
        return m_grant.getAccessTokensCodes();
    }

    @Override
    public List<RefreshToken> getRefreshTokens() {
        return m_grant.getRefreshTokens();
    }

    @Override
    public void setRefreshTokens(List<RefreshToken> refreshTokens) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public AccessToken getLongLivedAccessToken() {
        return m_grant.getLongLivedAccessToken();
    }

    @Override
    public IdToken getIdToken() {
        return m_grant.getIdToken();
    }

    @Override
    public JwtAuthorizationRequest getJwtAuthorizationRequest() {
        return m_grant.getJwtAuthorizationRequest();
    }

    @Override
    public void setJwtAuthorizationRequest(JwtAuthorizationRequest p_jwtAuthorizationRequest) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public Date getAuthenticationTime() {
        return m_grant.getAuthenticationTime();
    }

    @Override
    public TokenLdap getTokenLdap() {
        return m_grant.getTokenLdap();
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
    public String getAuthLevel() {
        return m_grant.getAuthLevel();
    }

    @Override
    public void setAuthLevel(String authLevel) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public String getAuthMode() {
        return m_grant.getAuthMode();
    }

    @Override
    public void setAuthMode(String authMode) {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException("Not allowed for UnmodifiableAuthorizationGrant.");
    }
}
