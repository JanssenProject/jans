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
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version 0.9, 08/14/2014
 */

public interface IAuthorizationGrant {

    public String getGrantId();

    public void setGrantId(String p_grantId);

    public AuthorizationCode getAuthorizationCode();

    public void setAuthorizationCode(AuthorizationCode authorizationCode);

    public String getNonce();

    public void setNonce(String nonce);

    public AccessToken createAccessToken();

    public AccessToken createLongLivedAccessToken();

    public RefreshToken createRefreshToken();

    public IdToken createIdToken(String nonce, AuthorizationCode authorizationCode, AccessToken accessToken,
                                 Map<String, String> claims, String authLevel, String authMode)
            throws SignatureException, StringEncrypter.EncryptionException, InvalidJwtException, InvalidJweException;

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

    public String getAuthLevel();

    public void setAuthLevel(String authLevel);

    public String getAuthMode();

    public void setAuthMode(String authMode);

    /**
     * Saves changes asynchronously
     */
    public void save();
}