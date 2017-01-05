/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import java.security.SignatureException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.xdi.oxauth.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.ldap.TokenLdap;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.token.IdTokenFactory;
import org.xdi.oxauth.model.token.JsonWebResponse;
import org.xdi.oxauth.service.GrantService;
import org.xdi.oxauth.util.TokenHashUtil;
import org.xdi.util.security.StringEncrypter;

/**
 * Base class for all the types of authorization grant.
 *
 * @author Javier Rojas Blum
 * @version November 11, 2016
 */
public class AuthorizationGrant extends AbstractAuthorizationGrant {

    private static final Logger LOGGER = Logger.getLogger(AuthorizationGrant.class);

    //private static final int THREADS_COUNT = 200;

//    private final ExecutorService executorService = Executors.newFixedThreadPool(THREADS_COUNT, new ThreadFactory() {
//        public Thread newThread(Runnable p_r) {
//            Thread thread = new Thread(p_r);
//            thread.setDaemon(true);
//            return thread;
//        }
//    });

    private final GrantService grantService = GrantService.instance();

    public AuthorizationGrant(User user, AuthorizationGrantType authorizationGrantType, Client client,
                              Date authenticationTime, AppConfiguration appConfiguration) {
        super(user, authorizationGrantType, client, authenticationTime, appConfiguration);
    }

    public static IdToken createIdToken(
            IAuthorizationGrant grant, String nonce, AuthorizationCode authorizationCode, AccessToken accessToken,
            Set<String> scopes, boolean includeIdTokenClaims) throws Exception {
        JsonWebResponse jwr = IdTokenFactory.createJwr(
                grant, nonce, authorizationCode, accessToken, scopes, includeIdTokenClaims);
        return new IdToken(jwr.toString(),
                jwr.getClaims().getClaimAsDate(JwtClaimName.ISSUED_AT),
                jwr.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME));
    }

    @Override
    public String checkScopesPolicy(String scope) {
        final String result = super.checkScopesPolicy(scope);
        save();
        return result;
    }

    @Override
    public void save() {
// http://ox.gluu.org/jira/browse/OXAUTH-322?focusedCommentId=11620#comment-11620
//        executorService.execute(new Runnable() {
//            @Override
//            public void run() {
        saveImpl();
//            }
//        });
    }

    private void saveImpl() {
        String grantId = getGrantId();
        if (grantId != null && StringUtils.isNotBlank(grantId)) {
            final List<TokenLdap> grants = grantService.getGrantsByGrantId(grantId);
            if (grants != null && !grants.isEmpty()) {
                final String nonce = getNonce();
                final String scopes = getScopesAsString();
                for (TokenLdap t : grants) {
                    t.setNonce(nonce);
                    t.setScope(scopes);
                    t.setAuthMode(getAcrValues());
                    t.setSessionDn(getSessionDn());
                    t.setAuthenticationTime(getAuthenticationTime() != null ? getAuthenticationTime().toString() : "");
                    t.setCodeChallenge(getCodeChallenge());
                    t.setCodeChallengeMethod(getCodeChallengeMethod());

                    final JwtAuthorizationRequest jwtRequest = getJwtAuthorizationRequest();
                    if (jwtRequest != null && StringUtils.isNotBlank(jwtRequest.getEncodedJwt())) {
                        t.setJwtRequest(jwtRequest.getEncodedJwt());
                    }
                    LOGGER.debug("Saving grant: " + grantId + ", code_challenge: " + getCodeChallenge());
                    grantService.mergeSilently(t);
                }
            }
        }
    }

    @Override
    public AccessToken createAccessToken() {
        try {
            final AccessToken accessToken = super.createAccessToken();
            if (accessToken.getExpiresIn() > 0) {
                persist(asToken(accessToken));
            }
            return accessToken;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public AccessToken createLongLivedAccessToken() {
        try {
            final AccessToken accessToken = super.createLongLivedAccessToken();
            if (accessToken.getExpiresIn() > 0) {
                persist(asToken(accessToken));
            }
            return accessToken;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public RefreshToken createRefreshToken() {
        try {
            final RefreshToken refreshToken = super.createRefreshToken();
            if (refreshToken.getExpiresIn() > 0) {
                persist(asToken(refreshToken));
            }
            return refreshToken;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public IdToken createIdToken(String nonce, AuthorizationCode authorizationCode, AccessToken accessToken,
                                 AuthorizationGrant authorizationGrant, boolean includeIdTokenClaims)
            throws SignatureException, StringEncrypter.EncryptionException, InvalidJwtException, InvalidJweException {
        try {
            final IdToken idToken = createIdToken(
                    this, nonce, authorizationCode, accessToken, getScopes(), includeIdTokenClaims);
            final String acrValues = authorizationGrant.getAcrValues();
            final String sessionDn = authorizationGrant.getSessionDn();
            if (idToken.getExpiresIn() > 0) {
                final TokenLdap tokenLdap = asToken(idToken);
                tokenLdap.setAuthMode(acrValues);
                tokenLdap.setSessionDn(sessionDn);
                persist(tokenLdap);
            }

            // is it really neccessary to propagate to all tokens?
            setAcrValues(acrValues);
            setSessionDn(sessionDn);
            save(); // asynchronous save
            return idToken;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    public void persist(TokenLdap p_token) {
        grantService.persist(p_token);
    }

    public void persist(AuthorizationCode p_code) {
        persist(asToken(p_code));
    }

    public TokenLdap asToken(IdToken p_token) {
        final TokenLdap result = asTokenLdap(p_token);
        result.setTokenTypeEnum(org.xdi.oxauth.model.ldap.TokenType.ID_TOKEN);
        return result;
    }

    public TokenLdap asToken(RefreshToken p_token) {
        final TokenLdap result = asTokenLdap(p_token);
        result.setTokenTypeEnum(org.xdi.oxauth.model.ldap.TokenType.REFRESH_TOKEN);
        return result;
    }

    public TokenLdap asToken(AuthorizationCode p_authorizationCode) {
        final TokenLdap result = asTokenLdap(p_authorizationCode);
        result.setTokenTypeEnum(org.xdi.oxauth.model.ldap.TokenType.AUTHORIZATION_CODE);
        return result;
    }

    public TokenLdap asToken(AccessToken p_accessToken) {
        final TokenLdap result = asTokenLdap(p_accessToken);
        result.setTokenTypeEnum(org.xdi.oxauth.model.ldap.TokenType.ACCESS_TOKEN);
        return result;
    }

    public String getScopesAsString() {
        final StringBuilder scopes = new StringBuilder();
        for (String s : getScopes()) {
            scopes.append(s).append(" ");
        }
        return scopes.toString().trim();
    }

    public TokenLdap asTokenLdap(AbstractToken p_token) {

        final String id = GrantService.generateGrantId();

        final TokenLdap result = new TokenLdap();

        result.setDn(grantService.buildDn(id, getGrantId(), getClientId()));
        result.setId(id);
        result.setGrantId(getGrantId());
        result.setCreationDate(p_token.getCreationDate());
        result.setExpirationDate(p_token.getExpirationDate());
        result.setTokenCode(TokenHashUtil.getHashedToken(p_token.getCode()));
        result.setUserId(getUserId());
        result.setClientId(getClientId());
        result.setScope(getScopesAsString());
        result.setAuthMode(p_token.getAuthMode());
        result.setSessionDn(p_token.getSessionDn());
        result.setAuthenticationTime(getAuthenticationTime() != null ? getAuthenticationTime().toString() : "");

        final AuthorizationGrantType grantType = getAuthorizationGrantType();
        if (grantType != null) {
            result.setGrantType(grantType.getParamName());
        }

        final AuthorizationCode authorizationCode = getAuthorizationCode();
        if (authorizationCode != null) {
            result.setAuthorizationCode(TokenHashUtil.getHashedToken(authorizationCode.getCode()));
        }

        final String nonce = getNonce();
        if (nonce != null) {
            result.setNonce(nonce);
        }

        final JwtAuthorizationRequest jwtRequest = getJwtAuthorizationRequest();
        if (jwtRequest != null && StringUtils.isNotBlank(jwtRequest.getEncodedJwt())) {
            result.setJwtRequest(jwtRequest.getEncodedJwt());
        }
        return result;
    }

    @Override
    public boolean isValid() {
//        final TokenLdap t = getTokenLdap();
//        if (t != null) {
//            if (new Date().after(t.getExpirationDate())) {
//                return true;
//            }
//        }
        return true;
    }

    @Override
    public void revokeAllTokens() {
        final TokenLdap tokenLdap = getTokenLdap();
        if (tokenLdap != null && StringUtils.isNotBlank(tokenLdap.getGrantId())) {
            grantService.removeAllByGrantId(tokenLdap.getGrantId());
        }
    }

    @Override
    public void checkExpiredTokens() {
        // do nothing, clean up is made via grant service: org.xdi.oxauth.service.GrantService.cleanUp()
    }
}