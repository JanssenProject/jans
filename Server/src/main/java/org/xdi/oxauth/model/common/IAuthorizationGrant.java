/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import com.google.common.base.Function;
import org.xdi.oxauth.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.model.exception.InvalidClaimException;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.ldap.TokenLdap;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.token.JsonWebResponse;
import org.xdi.util.security.StringEncrypter;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version June 28, 2017
 */

public interface IAuthorizationGrant {

    public String getGrantId();

    public void setGrantId(String p_grantId);

    public AuthorizationCode getAuthorizationCode();

    public void setAuthorizationCode(AuthorizationCode authorizationCode);

    public String getNonce();

    public void setNonce(String nonce);

    public AccessToken createAccessToken(String certAsPem);

    public RefreshToken createRefreshToken();

    public IdToken createIdToken(String nonce, AuthorizationCode authorizationCode, AccessToken accessToken,
                                 AuthorizationGrant authorizationGrant, boolean includeIdTokenClaims, Function<JsonWebResponse, Void> preProcessing)
            throws SignatureException, StringEncrypter.EncryptionException, InvalidJwtException, InvalidJweException,
            InvalidClaimException, InvalidKeyException, NoSuchAlgorithmException;

    public RefreshToken getRefreshToken(String refreshTokenCode);

    public AbstractToken getAccessToken(String tokenCode);

    public boolean isValid();

    public void revokeAllTokens();

    public void checkExpiredTokens();

    public String checkScopesPolicy(String scope);

    public User getUser();

    public String getUserId();

    public String getUserDn();

    public AuthorizationGrantType getAuthorizationGrantType();

    public String getClientId();

    public Client getClient();

    public String getClientDn();

    public List<AccessToken> getAccessTokens();

    public Set<String> getScopes();

    public Set<String> getRefreshTokensCodes();

    public Set<String> getAccessTokensCodes();

    public List<RefreshToken> getRefreshTokens();

    public void setRefreshTokens(List<RefreshToken> refreshTokens);

    public AccessToken getLongLivedAccessToken();

    public IdToken getIdToken();

    public JwtAuthorizationRequest getJwtAuthorizationRequest();

    public void setJwtAuthorizationRequest(JwtAuthorizationRequest p_jwtAuthorizationRequest);

    public Date getAuthenticationTime();

    public TokenLdap getTokenLdap();

    public void setTokenLdap(TokenLdap p_tokenLdap);

    public void setLongLivedAccessToken(AccessToken longLivedAccessToken);

    public void setIdToken(IdToken idToken);

    public void setScopes(Collection<String> scopes);

    public void setAccessTokens(List<AccessToken> accessTokens);

    public String getAcrValues();

    public void setAcrValues(String authMode);

    public String getSessionDn();

    public void setSessionDn(String sessionDn);

    /**
     * Saves changes asynchronously
     */
    public void save();
}