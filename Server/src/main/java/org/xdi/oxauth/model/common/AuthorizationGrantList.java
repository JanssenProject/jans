/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.util.Util;
import org.gluu.service.CacheService;
import org.slf4j.Logger;
import org.xdi.oxauth.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.model.ldap.TokenLdap;
import org.xdi.oxauth.model.ldap.TokenType;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.GrantService;
import org.xdi.oxauth.service.UserService;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.oxauth.util.TokenHashUtil;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Component to hold in memory authorization grant objects.
 *
 * @author Javier Rojas Blum
 * @version September 6, 2017
 */
@Dependent
public class AuthorizationGrantList implements IAuthorizationGrantList {

    @Inject
    private Logger log;

    @Inject
    private Instance<AbstractAuthorizationGrant> grantInstance;

    @Inject
    private GrantService grantService;

    @Inject
    private UserService userService;

    @Inject
    private ClientService clientService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private CacheService cacheService;

    @Override
    public void removeAuthorizationGrants(List<AuthorizationGrant> authorizationGrants) {
        if (authorizationGrants != null && !authorizationGrants.isEmpty()) {
            for (AuthorizationGrant r : authorizationGrants) {
                grantService.remove(r);
            }
        }
    }

    @Override
    public AuthorizationGrant createAuthorizationGrant(User user, Client client, Date authenticationTime) {
        AuthorizationGrant grant = grantInstance.select(SimpleAuthorizationGrant.class).get();
        grant.init(user, null, client, authenticationTime);

        return grant;
    }

    @Override
    public AuthorizationCodeGrant createAuthorizationCodeGrant(User user, Client client, Date authenticationTime) {
        AuthorizationCodeGrant grant = grantInstance.select(AuthorizationCodeGrant.class).get();
        grant.init(user, client, authenticationTime);

        CacheGrant memcachedGrant = new CacheGrant(grant, appConfiguration);
        cacheService.put(Integer.toString(grant.getAuthorizationCode().getExpiresIn()), memcachedGrant.cacheKey(), memcachedGrant);
        log.trace("Put authorization grant in cache, code: " + grant.getAuthorizationCode().getCode() + ", clientId: " + grant.getClientId());
        return grant;
    }

    @Override
    public ImplicitGrant createImplicitGrant(User user, Client client, Date authenticationTime) {
        ImplicitGrant grant = grantInstance.select(ImplicitGrant.class).get();
        grant.init(user, client, authenticationTime);

        return grant;
    }

    @Override
    public ClientCredentialsGrant createClientCredentialsGrant(User user, Client client) {
        ClientCredentialsGrant grant = grantInstance.select(ClientCredentialsGrant.class).get();
        grant.init(user, client);

        return grant;
    }

    @Override
    public ResourceOwnerPasswordCredentialsGrant createResourceOwnerPasswordCredentialsGrant(User user, Client client) {
        ResourceOwnerPasswordCredentialsGrant grant = grantInstance.select(ResourceOwnerPasswordCredentialsGrant.class).get();
        grant.init(user, client);

        return grant;
    }

    @Override
    public AuthorizationCodeGrant getAuthorizationCodeGrant(String clientId, String authorizationCode) {
        Object cachedGrant = cacheService.get(null, CacheGrant.cacheKey(clientId, authorizationCode, null));
        if (cachedGrant == null) {
            // retry one time : sometimes during high load cache client may be not fast enough
            cachedGrant = cacheService.get(null, CacheGrant.cacheKey(clientId, authorizationCode, null));
            log.trace("Failed to fetch authorization grant from cache, code: " + authorizationCode + ", clientId: " + clientId);
        }
        return cachedGrant instanceof CacheGrant ? ((CacheGrant) cachedGrant).asCodeGrant(grantInstance) : null;
    }

    @Override
    public AuthorizationGrant getAuthorizationGrantByRefreshToken(String clientId, String refreshTokenCode) {
        if (!ServerUtil.isTrue(appConfiguration.getPersistRefreshTokenInLdap())) {
            return assertTokenType((TokenLdap) cacheService.get(null, TokenHashUtil.getHashedToken(refreshTokenCode)), TokenType.REFRESH_TOKEN);
        }
        return assertTokenType(grantService.getGrantsByCodeAndClient(refreshTokenCode, clientId), TokenType.REFRESH_TOKEN);
    }

    public AuthorizationGrant assertTokenType(TokenLdap tokenLdap, TokenType tokenType) {
        if (tokenLdap != null && tokenLdap.getTokenTypeEnum() == tokenType) {
            return asGrant(tokenLdap);
        }
        return null;
    }

    @Override
    public List<AuthorizationGrant> getAuthorizationGrant(String clientId) {
        final List<AuthorizationGrant> result = new ArrayList<AuthorizationGrant>();
        try {
            final List<TokenLdap> entries = new ArrayList<TokenLdap>();
            entries.addAll(grantService.getGrantsOfClient(clientId));
            entries.addAll(grantService.getCacheClientTokensEntries(clientId));

            for (TokenLdap t : entries) {
                final AuthorizationGrant grant = asGrant(t);
                if (grant != null) {
                    result.add(grant);
                }
            }
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
        return result;
    }

    @Override
    public AuthorizationGrant getAuthorizationGrantByAccessToken(String accessToken) {
        return getAuthorizationGrantByAccessToken(accessToken, false);
    }

    public AuthorizationGrant getAuthorizationGrantByAccessToken(String accessToken, boolean onlyFromCache) {
        final TokenLdap tokenLdap = grantService.getGrantsByCode(accessToken, onlyFromCache);
        if (tokenLdap != null && (tokenLdap.getTokenTypeEnum() == org.xdi.oxauth.model.ldap.TokenType.ACCESS_TOKEN || tokenLdap.getTokenTypeEnum() == org.xdi.oxauth.model.ldap.TokenType.LONG_LIVED_ACCESS_TOKEN)) {
            return asGrant(tokenLdap);
        }
        return null;
    }

    @Override
    public AuthorizationGrant getAuthorizationGrantByIdToken(String idToken) {
        final TokenLdap tokenLdap = grantService.getGrantsByCode(idToken);
        if (tokenLdap != null && (tokenLdap.getTokenTypeEnum() == org.xdi.oxauth.model.ldap.TokenType.ID_TOKEN)) {
            return asGrant(tokenLdap);
        }
        return null;
    }

    public AuthorizationGrant load(String clientId, String p_code) {
        return asGrant(grantService.getGrantsByCodeAndClient(p_code, clientId));
    }

    public static String extractClientIdFromTokenDn(String p_dn) {
        return StringUtils.substringBetween(p_dn, "inum=", "ou=clients").replaceAll(",", "");
    }

    public AuthorizationGrant asGrant(TokenLdap tokenLdap) {
        if (tokenLdap != null) {
            final AuthorizationGrantType grantType = AuthorizationGrantType.fromString(tokenLdap.getGrantType());
            if (grantType != null) {
                final User user = userService.getUser(tokenLdap.getUserId());
                final Client client = clientService.getClient(extractClientIdFromTokenDn(tokenLdap.getDn()));
                final Date authenticationTime = tokenLdap.getAuthenticationTime();
                final String nonce = tokenLdap.getNonce();

                AuthorizationGrant result;
                switch (grantType) {
                    case AUTHORIZATION_CODE:
                        AuthorizationCodeGrant authorizationCodeGrant = grantInstance.select(AuthorizationCodeGrant.class).get();
                        authorizationCodeGrant.init(user, client, authenticationTime);

                        result = authorizationCodeGrant;
                        break;
                    case CLIENT_CREDENTIALS:
                        ClientCredentialsGrant clientCredentialsGrant = grantInstance.select(ClientCredentialsGrant.class).get();
                        clientCredentialsGrant.init(user, client);

                        result = clientCredentialsGrant;
                        break;
                    case IMPLICIT:
                        ImplicitGrant implicitGrant = grantInstance.select(ImplicitGrant.class).get();
                        implicitGrant.init(user, client, authenticationTime);

                        result = implicitGrant;
                        break;
                    case RESOURCE_OWNER_PASSWORD_CREDENTIALS:
                        ResourceOwnerPasswordCredentialsGrant resourceOwnerPasswordCredentialsGrant = grantInstance.select(ResourceOwnerPasswordCredentialsGrant.class).get();
                        resourceOwnerPasswordCredentialsGrant.init(user, client);

                        result = resourceOwnerPasswordCredentialsGrant;
                        break;
                    default:
                        return null;
                }

                final String grantId = tokenLdap.getGrantId();
                final String jwtRequest = tokenLdap.getJwtRequest();
                final String authMode = tokenLdap.getAuthMode();
                final String sessionDn = tokenLdap.getSessionDn();
                final String claims = tokenLdap.getClaims();

                result.setTokenBindingHash(tokenLdap.getTokenBindingHash());
                result.setNonce(nonce);
                result.setX5cs256(tokenLdap.getAttributes().getX5cs256());
                result.setTokenLdap(tokenLdap);
                if (StringUtils.isNotBlank(grantId)) {
                    result.setGrantId(grantId);
                }
                result.setScopes(Util.splittedStringAsList(tokenLdap.getScope(), " "));

                result.setCodeChallenge(tokenLdap.getCodeChallenge());
                result.setCodeChallengeMethod(tokenLdap.getCodeChallengeMethod());

                if (StringUtils.isNotBlank(jwtRequest)) {
                    try {
                        result.setJwtAuthorizationRequest(new JwtAuthorizationRequest(appConfiguration, jwtRequest, client));
                    } catch (Exception e) {
                        log.trace(e.getMessage(), e);
                    }
                }

                result.setAcrValues(authMode);
                result.setSessionDn(sessionDn);
                result.setClaims(claims);

                if (tokenLdap.getTokenTypeEnum() != null) {
                    switch (tokenLdap.getTokenTypeEnum()) {
                        case AUTHORIZATION_CODE:
                            if (result instanceof AuthorizationCodeGrant) {
                                final AuthorizationCode code = new AuthorizationCode(tokenLdap.getTokenCode(), tokenLdap.getCreationDate(), tokenLdap.getExpirationDate());
                                final AuthorizationCodeGrant g = (AuthorizationCodeGrant) result;
                                g.setAuthorizationCode(code);
                            }
                            break;
                        case REFRESH_TOKEN:
                            final RefreshToken refreshToken = new RefreshToken(tokenLdap.getTokenCode(), tokenLdap.getCreationDate(), tokenLdap.getExpirationDate());
                            result.setRefreshTokens(Arrays.asList(refreshToken));
                            break;
                        case ACCESS_TOKEN:
                            final AccessToken accessToken = new AccessToken(tokenLdap.getTokenCode(), tokenLdap.getCreationDate(), tokenLdap.getExpirationDate());
                            result.setAccessTokens(Arrays.asList(accessToken));
                            break;
                        case ID_TOKEN:
                            final IdToken idToken = new IdToken(tokenLdap.getTokenCode(), tokenLdap.getCreationDate(), tokenLdap.getExpirationDate());
                            result.setIdToken(idToken);
                            break;
                        case LONG_LIVED_ACCESS_TOKEN:
                            final AccessToken longLivedAccessToken = new AccessToken(tokenLdap.getTokenCode(), tokenLdap.getCreationDate(), tokenLdap.getExpirationDate());
                            result.setLongLivedAccessToken(longLivedAccessToken);
                            break;
                    }
                }
                return result;
            }
        }
        return null;
    }

}
