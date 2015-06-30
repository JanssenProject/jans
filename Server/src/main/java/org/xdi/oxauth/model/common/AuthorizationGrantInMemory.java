/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import org.xdi.oxauth.model.exception.InvalidClaimException;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwe.Jwe;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.token.IdTokenFactory;
import org.xdi.util.security.StringEncrypter;

import java.security.SignatureException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

/**
 * Base class for all the types of authorization grant.
 *
 * @author Javier Rojas Blum
 * @version June 3, 2015
 */

public class AuthorizationGrantInMemory extends AbstractAuthorizationGrant {

//    private static final Logger LOGGER = Logger.getLogger(AuthorizationGrantInMemory.class);

    private TokenIssuerObserver tokenIssuerObserver;
    private AuthorizationGrant parentRef;

    /**
     * @param user                   The resource owner.
     * @param authorizationGrantType The authorization grant type.
     * @param client                 An application making protected resource requests on behalf
     *                               of the resource owner and with its authorization.
     * @param authenticationTime     The Claim Value is the number of seconds from
     *                               1970-01-01T0:0:0Z as measured in UTC until the date/time that the
     *                               End-User authentication occurred.
     */
    public AuthorizationGrantInMemory(User user, AuthorizationGrantType authorizationGrantType, Client client,
                                      Date authenticationTime) {
        super(user, authorizationGrantType, client, authenticationTime);
    }

    /**
     * Creates a new {@link AccessToken}. By default the token has the bearer
     * token type.
     *
     * @return The access token.
     */
    @Override
    public AccessToken createAccessToken() {
        AccessToken accessToken = super.createAccessToken();
        accessTokens.put(accessToken.getCode(), accessToken);

        if (tokenIssuerObserver != null) {
            tokenIssuerObserver.indexByAccessToken(accessToken, this.getParentRef());
        }

        return accessToken;
    }

    @Override
    public AccessToken createLongLivedAccessToken() {
        if (getLongLivedAccessToken() == null) {
            setLongLivedAccessToken(super.createLongLivedAccessToken());
        } else {
            GregorianCalendar currentDate = new GregorianCalendar();
            GregorianCalendar issueDate = new GregorianCalendar();
            issueDate.setTime(getLongLivedAccessToken().getCreationDate());
            issueDate.add(Calendar.HOUR, 24);

            if (issueDate.before(currentDate)) {
                setLongLivedAccessToken(super.createLongLivedAccessToken());
            }
        }

        if (tokenIssuerObserver != null) {
            tokenIssuerObserver.indexByAccessToken(getLongLivedAccessToken(), this.getParentRef());
        }

        return getLongLivedAccessToken();
    }

    /**
     * Creates a new {@link RefreshToken} and revokes all the old refresh
     * tokens.
     *
     * @return The refresh token.
     */
    @Override
    public RefreshToken createRefreshToken() {
//        refreshTokens.clear(); // instead of revoke just remove, is there any reason to keep it in memory?
        for (RefreshToken refToken : refreshTokens.values()) {
            refToken.setRevoked(true);
        }

        RefreshToken refreshToken = super.createRefreshToken();
        refreshTokens.put(refreshToken.getCode(), refreshToken);

        if (tokenIssuerObserver != null) {
            tokenIssuerObserver.indexByRefreshToken(refreshToken, this.getParentRef());
        }

        return refreshToken;
    }

    /**
     * Creates a new {@link IdToken} or return an existent one.
     *
     * @param nonce The id token.
     * @return The id token.
     */
    @Override
    public IdToken createIdToken(
            String nonce, AuthorizationCode authorizationCode, AccessToken accessToken, String authMode)
            throws SignatureException, StringEncrypter.EncryptionException, InvalidJwtException, InvalidJweException,
            InvalidClaimException {
        if (getIdToken() == null) {
            IdToken idToken = createIdToken(this, nonce, authorizationCode, accessToken, getScopes());
            setIdToken(idToken);
            if (tokenIssuerObserver != null) {
                tokenIssuerObserver.indexByIdToken(idToken, this.getParentRef());
            }
        }

        return getIdToken();
    }

    public static IdToken createIdToken(
            IAuthorizationGrant p_grant, String nonce, AuthorizationCode authorizationCode, AccessToken accessToken,
            Set<String> scopes)
            throws InvalidJweException, SignatureException, StringEncrypter.EncryptionException, InvalidJwtException,
            InvalidClaimException {
    	IdTokenFactory idTokenFactory = IdTokenFactory.instance();

    	final Client grantClient = p_grant.getClient();
        if (grantClient != null && grantClient.getIdTokenEncryptedResponseAlg() != null
                && grantClient.getIdTokenEncryptedResponseEnc() != null) {
            Jwe jwe = idTokenFactory.generateEncryptedIdToken(p_grant, nonce, authorizationCode, accessToken, scopes);
            return new IdToken(jwe.toString(),
                    jwe.getClaims().getClaimAsDate(JwtClaimName.ISSUED_AT),
                    jwe.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME));
        } else {
            Jwt jwt = idTokenFactory.generateSignedIdToken(p_grant, nonce, authorizationCode, accessToken, scopes);
            return new IdToken(jwt.toString(),
                    jwt.getClaims().getClaimAsDate(JwtClaimName.ISSUED_AT),
                    jwt.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME));
        }
    }

    @Override
    public boolean isValid() {
        checkExpiredTokens();

        for (AccessToken accessToken : accessTokens.values()) {
            if (accessToken.isValid()) {
                return true;
            }
        }
        for (RefreshToken refreshToken : refreshTokens.values()) {
            if (refreshToken.isValid()) {
                return true;
            }
        }
        final IdToken idToken = getIdToken();
        if (idToken != null && idToken.isValid()) {
            return true;
        }
        final AccessToken longLivedAccessToken = getLongLivedAccessToken();
        return longLivedAccessToken != null && longLivedAccessToken.isValid();

    }

    /**
     * Revokes all the issued tokens.
     */
    @Override
    public void revokeAllTokens() {
        // instead of revoke just remove, is there any reason to keep it in memory?

//        accessTokens.clear();
//        refreshTokens.clear();
//        idToken = null;
//        longLivedAccessToken = null;

        for (AccessToken accessToken : accessTokens.values()) {
            accessToken.setRevoked(true);
        }
        for (RefreshToken refreshToken : refreshTokens.values()) {
            refreshToken.setRevoked(true);
        }
        if (getIdToken() != null) {
            getIdToken().setRevoked(true);
        }
        if (getLongLivedAccessToken() != null) {
            getLongLivedAccessToken().setRevoked(true);
        }
    }

    /**
     * Check all tokens for expiration.
     */
    @Override
    public void checkExpiredTokens() {
        for (AccessToken accessToken : accessTokens.values()) {
            accessToken.checkExpired();
        }
        for (RefreshToken refreshToken : refreshTokens.values()) {
            refreshToken.checkExpired();
        }
        if (getIdToken() != null) {
            getIdToken().checkExpired();
        }
        if (getLongLivedAccessToken() != null) {
            getLongLivedAccessToken().checkExpired();
        }
    }

    @Override
    public void save() {
        // do nothing
    }

    public void setTokenIssuerObserver(TokenIssuerObserver tokenIssuerObserver) {
        this.tokenIssuerObserver = tokenIssuerObserver;
    }

    public AuthorizationGrant getParentRef() {
        return parentRef;
    }

    public void setParentRef(AuthorizationGrant p_parentRef) {
        parentRef = p_parentRef;
    }
}