/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.oxauth.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.exception.InvalidClaimException;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.ldap.TokenLdap;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.util.security.StringEncrypter;

import java.security.SignatureException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Base class for all the types of authorization grant.
 *
 * @author Javier Rojas Blum
 * @version June 3, 2015
 */
public class AuthorizationGrant implements IAuthorizationGrant {

    private static final Log LOGGER = Logging.getLog(AuthorizationGrantListLdap.class);

    private IAuthorizationGrant grant;

    /**
     * @param user                   The resource owner.
     * @param authorizationGrantType The authorization grant type.
     * @param client                 An application making protected resource requests on behalf
     *                               of the resource owner and with its authorization.
     * @param authenticationTime     The Claim Value is the number of seconds from
     *                               1970-01-01T0:0:0Z as measured in UTC until the date/time that the
     *                               End-User authentication occurred.
     */
    public AuthorizationGrant(User user, AuthorizationGrantType authorizationGrantType, Client client,
                              Date authenticationTime) {
        switch (ConfigurationFactory.instance().getConfiguration().getModeEnum()) {
            case IN_MEMORY:
                grant = new AuthorizationGrantInMemory(user, authorizationGrantType, client, authenticationTime);
                ((AuthorizationGrantInMemory) grant).setParentRef(this);
                break;
            case LDAP:
                grant = new AuthorizationGrantLdap(user, authorizationGrantType, client, authenticationTime);
                break;
            default:
                LOGGER.error("Unable to identify mode of the server. (Please check configuration.)");
                throw new IllegalArgumentException("Unable to identify mode of the server. (Please check configuration.) " + ConfigurationFactory.instance().getConfiguration().getModeEnum());
        }
    }

    public IAuthorizationGrant getGrant() {
        return grant;
    }

    @Override
    public String getGrantId() {
        return grant.getGrantId();
    }

    @Override
    public void setGrantId(String p_grantId) {
        grant.setGrantId(p_grantId);
    }

    @Override
    public AuthorizationCode getAuthorizationCode() {
        return grant.getAuthorizationCode();
    }

    @Override
    public void setAuthorizationCode(AuthorizationCode authorizationCode) {
        grant.setAuthorizationCode(authorizationCode);
    }

    @Override
    public String getNonce() {
        return grant.getNonce();
    }

    @Override
    public void setNonce(String nonce) {
        grant.setNonce(nonce);
    }

    /**
     * Creates a new {@link AccessToken}. By default the token has the bearer
     * token type.
     *
     * @return The access token.
     */
    @Override
    public AccessToken createAccessToken() {
        return grant.createAccessToken();
    }

    @Override
    public AccessToken createLongLivedAccessToken() {
        return grant.createLongLivedAccessToken();
    }

    /**
     * Creates a new {@link RefreshToken} and revokes all the old refresh
     * tokens.
     *
     * @return The refresh token.
     */
    @Override
    public RefreshToken createRefreshToken() {
        return grant.createRefreshToken();
    }

    /**
     * Creates a new {@link IdToken} or return an existent one.
     *
     * @param nonce The id token.
     * @return The id token.
     */
    @Override
    public IdToken createIdToken(String nonce, AuthorizationCode authorizationCode, AccessToken accessToken,
                                 String authMode)
            throws SignatureException, StringEncrypter.EncryptionException, InvalidJwtException, InvalidJweException,
            InvalidClaimException {
        return grant.createIdToken(nonce, authorizationCode, accessToken, authMode);
    }

    /**
     * Gets the refresh token instance from the refresh token list given its
     * code.
     *
     * @param refreshTokenCode The code of the refresh token.
     * @return The refresh token instance or
     * <code>null</code> if not found.
     */
    @Override
    public RefreshToken getRefreshToken(String refreshTokenCode) {
        return grant.getRefreshToken(refreshTokenCode);
    }

    /**
     * Gets the access token instance from the id token list or the access token
     * list given its code.
     *
     * @param tokenCode The code of the access token.
     * @return The access token instance or
     * <code>null</code> if not found.
     */
    @Override
    public AbstractToken getAccessToken(String tokenCode) {
        return grant.getAccessToken(tokenCode);
    }

    @Override
    public boolean isValid() {
        return grant.isValid();
    }

    /**
     * Revokes all the issued tokens.
     */
    @Override
    public void revokeAllTokens() {
        grant.revokeAllTokens();
    }

    /**
     * Check all tokens for expiration.
     */
    @Override
    public void checkExpiredTokens() {
        grant.checkExpiredTokens();
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
    public String checkScopesPolicy(String scope) {
        return grant.checkScopesPolicy(scope);
    }

    /**
     * Returns the resource owner's.
     *
     * @return The resource owner's.
     */
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

    /**
     * Returns the {@link AuthorizationGrantType}.
     *
     * @return The authorization grant type.
     */
    @Override
    public AuthorizationGrantType getAuthorizationGrantType() {
        return grant.getAuthorizationGrantType();
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
        return grant.getClient();
    }

    @Override
    public String getClientId() {
        return grant.getClientId();
    }

    @Override
    public String getClientDn() {
        return grant.getClientDn();
    }

    /**
     * Returns a list with all the issued access tokens.
     *
     * @return List with all the issued access tokens.
     */
    @Override
    public List<AccessToken> getAccessTokens() {
        return grant.getAccessTokens();
    }

    /**
     * Returns a list with all the issued refresh tokens codes.
     *
     * @return List with all the issued refresh tokens codes.
     */
    @Override
    public Set<String> getRefreshTokensCodes() {
        return grant.getRefreshTokensCodes();
    }

    /**
     * Returns a list with all the issued access tokens codes.
     *
     * @return List with all the issued access tokens codes.
     */
    @Override
    public Set<String> getAccessTokensCodes() {
        return grant.getAccessTokensCodes();
    }

    /**
     * Returns a list with all the issued refresh tokens.
     *
     * @return List with all the issued refresh tokens.
     */
    @Override
    public List<RefreshToken> getRefreshTokens() {
        return grant.getRefreshTokens();
    }

    @Override
    public void setRefreshTokens(List<RefreshToken> refreshTokens) {
        grant.setRefreshTokens(refreshTokens);
    }

    @Override
    public AccessToken getLongLivedAccessToken() {
        return grant.getLongLivedAccessToken();
    }

    @Override
    public IdToken getIdToken() {
        return grant.getIdToken();
    }

    /**
     * Returns a list of the scopes granted to the client.
     *
     * @return List of the scopes granted to the client.
     */
    @Override
    public Set<String> getScopes() {
        return grant.getScopes();
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
        grant.setTokenLdap(p_tokenLdap);
    }

    @Override
    public void setLongLivedAccessToken(AccessToken longLivedAccessToken) {
        grant.setLongLivedAccessToken(longLivedAccessToken);
    }

    @Override
    public void setIdToken(IdToken idToken) {
        grant.setIdToken(idToken);
    }

    @Override
    public void setScopes(Collection<String> scopes) {
        grant.setScopes(scopes);
    }

    @Override
    public void setAccessTokens(List<AccessToken> accessTokens) {
        grant.setAccessTokens(accessTokens);
    }

    @Override
    public JwtAuthorizationRequest getJwtAuthorizationRequest() {
        return grant.getJwtAuthorizationRequest();
    }

    @Override
    public void setJwtAuthorizationRequest(JwtAuthorizationRequest p_jwtAuthorizationRequest) {
        grant.setJwtAuthorizationRequest(p_jwtAuthorizationRequest);
    }

    @Override
    public void setAcrValues(String authMode) {
        grant.setAcrValues(authMode);
    }

    /**
     * Saves changes asynchronously
     */
    @Override
    public void save() {
        grant.save();
    }

    @Override
    public String getAcrValues() {
        return grant.getAcrValues();
    }

}