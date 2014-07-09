package org.xdi.oxauth.model.common;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.xdi.oxauth.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.ldap.TokenLdap;
import org.xdi.oxauth.model.ldap.TokenType;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.service.GrantService;
import org.xdi.util.security.StringEncrypter;

import java.security.SignatureException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version 0.9, 08/14/2014
 */

public class AuthorizationGrantLdap extends AbstractAuthorizationGrant {

    private static final Logger LOGGER = Logger.getLogger(AuthorizationGrantLdap.class);

    private final GrantService m_grantService = GrantService.instance();

    public AuthorizationGrantLdap(User user, AuthorizationGrantType authorizationGrantType, Client client,
                                  Date authenticationTime) {
        super(user, authorizationGrantType, client, authenticationTime);
    }

    @Override
    public String checkScopesPolicy(String scope) {
        final String result = super.checkScopesPolicy(scope);
        save();
        // yuriyz: Check, maybe store scopes in asynchronous call to release thread faster
//        Executors.newSingleThreadExecutor().execute(new Runnable() {
//            @Override
//            public void run() {
//                save();
//            }
//        });
        return result;
    }

    @Override
    public void save() {
        String grantId = getGrantId();
        if (grantId != null && StringUtils.isNotBlank(grantId)) {
            final List<TokenLdap> grants = m_grantService.getGrantsByGrantId(grantId);
            if (grants != null && !grants.isEmpty()) {
                final String nonce = getNonce();
                final String scopes = getScopesAsString();
                for (TokenLdap t : grants) {
                    t.setNonce(nonce);
                    t.setScope(scopes);
                    t.setAuthLevel(getAuthLevel());
                    t.setAuthMode(getAuthMode());
                    t.setAuthenticationTime(getAuthenticationTime() != null ? getAuthenticationTime().toString() : "");

                    final JwtAuthorizationRequest jwtRequest = getJwtAuthorizationRequest();
                    if (jwtRequest != null && StringUtils.isNotBlank(jwtRequest.getEncodedJwt())) {
                        t.setJwtRequest(jwtRequest.getEncodedJwt());
                    }
                    m_grantService.mergeSilently(t);
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
            LOGGER.trace(e.getMessage(), e);
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
            LOGGER.trace(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public RefreshToken createRefreshToken() {
        try {
            final RefreshToken refreshToken = super.createRefreshToken();
            persist(asToken(refreshToken));
            return refreshToken;
        } catch (Exception e) {
            LOGGER.trace(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public IdToken createIdToken(String nonce, AuthorizationCode authorizationCode, AccessToken accessToken,
                                 Map<String, String> claims) throws SignatureException, StringEncrypter.EncryptionException, InvalidJwtException, InvalidJweException {
        try {
            final IdToken idToken = AuthorizationGrantInMemory.createIdToken(this, nonce, authorizationCode, accessToken, claims);
        	if (idToken.getExpiresIn() > 0) {
        		persist(asToken(idToken));
        	}
            return idToken;
        } catch (Exception e) {
            LOGGER.trace(e.getMessage(), e);
            return null;
        }
    }

    public void persist(TokenLdap p_token) {
        m_grantService.persist(p_token);
    }

    public void persist(AuthorizationCode p_code) {
        persist(asToken(p_code));
    }

    public TokenLdap asToken(IdToken p_token) {
        final TokenLdap result = asTokenLdap(p_token);
        result.setTokenTypeEnum(TokenType.ID_TOKEN);
        return result;
    }

    public TokenLdap asToken(RefreshToken p_token) {
        final TokenLdap result = asTokenLdap(p_token);
        result.setTokenTypeEnum(TokenType.REFRESH_TOKEN);
        return result;
    }

    public TokenLdap asToken(AuthorizationCode p_authorizationCode) {
        final TokenLdap result = asTokenLdap(p_authorizationCode);
        result.setTokenTypeEnum(TokenType.AUTHORIZATION_CODE);
        return result;
    }

    public TokenLdap asToken(AccessToken p_accessToken) {
        final TokenLdap result = asTokenLdap(p_accessToken);
        result.setTokenTypeEnum(TokenType.ACCESS_TOKEN);
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

        result.setDn(GrantService.buildDn(id, getClientId()));
        result.setId(id);
        result.setGrantId(getGrantId());
        result.setCreationDate(p_token.getCreationDate());
        result.setExpirationDate(p_token.getExpirationDate());
        result.setTokenCode(p_token.getCode());
        result.setUserId(getUserId());
        result.setScope(getScopesAsString());
        result.setAuthLevel(p_token.getAuthLevel());
        result.setAuthMode(p_token.getAuthMode());
        result.setAuthenticationTime(getAuthenticationTime() != null ? getAuthenticationTime().toString() : "");

        final AuthorizationGrantType grantType = getAuthorizationGrantType();
        if (grantType != null) {
            result.setGrantType(grantType.getParamName());
        }

        final AuthorizationCode authorizationCode = getAuthorizationCode();
        if (authorizationCode != null) {
            result.setAuthorizationCode(authorizationCode.getCode());
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
            m_grantService.removeAllByGrantId(tokenLdap.getGrantId());
        }
    }

    @Override
    public void checkExpiredTokens() {
        // do nothing, clean up is made via grant service: org.xdi.oxauth.service.GrantService.cleanUp()
    }
}
