/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.util.Util;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.as.server.model.ldap.TokenEntity;
import io.jans.as.server.model.ldap.TokenType;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.GrantService;
import io.jans.as.server.service.MetricService;
import io.jans.as.server.util.TokenHashUtil;
import io.jans.model.metric.MetricType;
import io.jans.service.CacheService;
import io.jans.util.StringHelper;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static io.jans.as.model.util.Util.escapeLog;
import static org.apache.commons.lang.BooleanUtils.isFalse;

/**
 * Component to hold in memory authorization grant objects.
 *
 * @author Javier Rojas Blum
 * @version October 12, 2021
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

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private MetricService metricService;

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
        cacheService.put(grant.getAuthorizationCode().getExpiresIn(), memcachedGrant.cacheKey(), memcachedGrant);

        final String escapedCode = escapeLog(grant.getAuthorizationCode().getCode());
        final String escapedClientId = escapeLog(grant.getClientId());
        log.trace("Put authorization grant in cache, code: {}, clientId: {}", escapedCode, escapedClientId);

        metricService.incCounter(MetricType.TOKEN_AUTHORIZATION_CODE_COUNT);
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
    public TokenExchangeGrant createTokenExchangeGrant(User user, Client client) {
        TokenExchangeGrant grant = grantInstance.select(TokenExchangeGrant.class).get();
        grant.init(user, client);
        return grant;
    }

    @Override
    public CIBAGrant createCIBAGrant(CibaRequestCacheControl request) {
        CIBAGrant grant = grantInstance.select(CIBAGrant.class).get();
        grant.init(request);

        CacheGrant memcachedGrant = new CacheGrant(grant, appConfiguration);
        cacheService.put(request.getExpiresIn(), memcachedGrant.getAuthReqId(), memcachedGrant);

        final String escapedAuthReqId = escapeLog(grant.getAuthReqId());
        final String escapedGrantId = escapeLog(grant.getGrantId());
        log.trace("Ciba grant saved in cache, authReqId: {}, grantId: {}", escapedAuthReqId, escapedGrantId);

        return grant;
    }

    @Override
    public CIBAGrant getCIBAGrant(String authReqId) {
        Object cachedGrant = cacheService.get(authReqId);
        if (cachedGrant == null) {
            // retry one time : sometimes during high load cache client may be not fast enough
            cachedGrant = cacheService.get(authReqId);

            final String escapedAuthReqId = escapeLog(authReqId);
            log.trace("Failed to fetch CIBA grant from cache, authReqId: {}", escapedAuthReqId);
        }
        return cachedGrant instanceof CacheGrant ? ((CacheGrant) cachedGrant).asCibaGrant(grantInstance) : null;
    }

    @Override
    public DeviceCodeGrant createDeviceGrant(DeviceAuthorizationCacheControl data, User user) {
        DeviceCodeGrant grant = grantInstance.select(DeviceCodeGrant.class).get();
        grant.init(data, user);

        CacheGrant memcachedGrant = new CacheGrant(grant, appConfiguration);
        cacheService.put(data.getExpiresIn(), memcachedGrant.getDeviceCode(), memcachedGrant);

        final String escapedDeviceCode = escapeLog(grant.getDeviceCode());
        final String escapedGrantId = escapeLog(grant.getGrantId());
        log.trace("Device code grant saved in cache, deviceCode: {}, grantId: {}", escapedDeviceCode, escapedGrantId);
        return grant;
    }

    @Override
    public DeviceCodeGrant getDeviceCodeGrant(String deviceCode) {
        Object cachedGrant = cacheService.get(deviceCode);
        if (cachedGrant == null) {
            // retry one time : sometimes during high load cache client may be not fast enough
            cachedGrant = cacheService.get(deviceCode);

            final String escapedDeviceCode = escapeLog(deviceCode);
            log.trace("Failed to fetch Device code grant from cache, deviceCode: {}", escapedDeviceCode);
        }
        return cachedGrant instanceof CacheGrant ? ((CacheGrant) cachedGrant).asDeviceCodeGrant(grantInstance) : null;
    }

    @Override
    public AuthorizationCodeGrant getAuthorizationCodeGrant(String authorizationCode) {
        Object cachedGrant = cacheService.get(CacheGrant.cacheKey(authorizationCode, null));
        if (cachedGrant == null) {
            // retry one time : sometimes during high load cache client may be not fast enough
            cachedGrant = cacheService.get(CacheGrant.cacheKey(authorizationCode, null));

            final String escapedAuthorizationCode = escapeLog(authorizationCode);
            log.trace("Failed to fetch authorization grant from cache, code: {}", escapedAuthorizationCode);
        }
        return cachedGrant instanceof CacheGrant ? ((CacheGrant) cachedGrant).asCodeGrant(grantInstance) : null;
    }

    @Override
    public AuthorizationGrant getAuthorizationGrantByRefreshToken(String clientId, String refreshTokenCode) {
        if (isFalse(appConfiguration.getPersistRefreshToken())) {
            return assertTokenType((TokenEntity) cacheService.get(TokenHashUtil.hash(refreshTokenCode)), io.jans.as.server.model.ldap.TokenType.REFRESH_TOKEN, clientId);
        }
        return assertTokenType(grantService.getGrantByCode(refreshTokenCode), io.jans.as.server.model.ldap.TokenType.REFRESH_TOKEN, clientId);
    }

    public AuthorizationGrant assertTokenType(TokenEntity tokenEntity, TokenType tokenType, String clientId) {
        if (tokenEntity == null || tokenEntity.getTokenTypeEnum() != tokenType) {
            return null;
        }

        final AuthorizationGrant grant = asGrant(tokenEntity);
        if (grant == null || !grant.getClientId().equals(clientId)) {
            return null;
        }
        return grant;
    }

    @Override
    public List<AuthorizationGrant> getAuthorizationGrant(String clientId) {
        final List<AuthorizationGrant> result = new ArrayList<>();
        try {
            final List<TokenEntity> entries = new ArrayList<>(grantService.getGrantsOfClient(clientId));

            for (TokenEntity t : entries) {
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
        final TokenEntity tokenEntity = grantService.getGrantByCode(accessToken);
        if (tokenEntity != null && (tokenEntity.getTokenTypeEnum() == io.jans.as.server.model.ldap.TokenType.ACCESS_TOKEN || tokenEntity.getTokenTypeEnum() == io.jans.as.server.model.ldap.TokenType.LONG_LIVED_ACCESS_TOKEN)) {
            return asGrant(tokenEntity);
        }
        return null;
    }

    @Override
    public AuthorizationGrant getAuthorizationGrantByIdToken(String idToken) {
        if (StringUtils.isBlank(idToken)) {
            return null;
        }
        final TokenEntity tokenEntity = grantService.getGrantByCode(idToken);
        if (tokenEntity != null && (tokenEntity.getTokenTypeEnum() == io.jans.as.server.model.ldap.TokenType.ID_TOKEN)) {
            return asGrant(tokenEntity);
        }
        return null;
    }

    public AuthorizationGrant asGrant(TokenEntity tokenEntity) {
        if (tokenEntity != null) {
            final AuthorizationGrantType grantType = AuthorizationGrantType.fromString(tokenEntity.getGrantType());
            if (grantType != null) {
            	String userId = tokenEntity.getUserId();
            	User user = null;
            	if (StringHelper.isNotEmpty(userId)) {
                    user = userService.getUser(userId);
            	}
                final Client client = clientService.getClient(tokenEntity.getClientId());
                final Date authenticationTime = tokenEntity.getAuthenticationTime();
                final String nonce = tokenEntity.getNonce();

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
                    case CIBA:
                        CIBAGrant cibaGrant = grantInstance.select(CIBAGrant.class).get();
                        cibaGrant.init(user, AuthorizationGrantType.CIBA, client, tokenEntity.getCreationDate());

                        result = cibaGrant;
                        break;
                    case DEVICE_CODE:
                        DeviceCodeGrant deviceCodeGrant = grantInstance.select(DeviceCodeGrant.class).get();
                        deviceCodeGrant.init(user, AuthorizationGrantType.DEVICE_CODE, client, tokenEntity.getCreationDate());

                        result = deviceCodeGrant;
                        break;
                    case TOKEN_EXCHANGE:
                        TokenExchangeGrant tokenExchangeGrant = grantInstance.select(TokenExchangeGrant.class).get();
                        tokenExchangeGrant.init(user, AuthorizationGrantType.TOKEN_EXCHANGE, client, tokenEntity.getCreationDate());

                        result = tokenExchangeGrant;
                        break;
                    default:
                        return null;
                }

                final String grantId = tokenEntity.getGrantId();
                final String jwtRequest = tokenEntity.getJwtRequest();
                final String authMode = tokenEntity.getAuthMode();
                final String sessionDn = tokenEntity.getSessionDn();
                final String claims = tokenEntity.getClaims();

                result.setTokenBindingHash(tokenEntity.getTokenBindingHash());
                result.setNonce(nonce);
                result.setX5cs256(tokenEntity.getAttributes().getX5cs256());
                result.setTokenEntity(tokenEntity);
                if (StringUtils.isNotBlank(grantId)) {
                    result.setGrantId(grantId);
                }
                result.setScopes(Util.splittedStringAsList(tokenEntity.getScope(), " "));

                result.setCodeChallenge(tokenEntity.getCodeChallenge());
                result.setCodeChallengeMethod(tokenEntity.getCodeChallengeMethod());

                if (StringUtils.isNotBlank(jwtRequest)) {
                    try {
                        result.setJwtAuthorizationRequest(new JwtAuthorizationRequest(appConfiguration, cryptoProvider, jwtRequest, client));
                    } catch (Exception e) {
                        log.trace(e.getMessage(), e);
                    }
                }

                result.setAcrValues(authMode);
                result.setSessionDn(sessionDn);
                result.setClaims(claims);

                if (tokenEntity.getTokenTypeEnum() != null) {
                    switch (tokenEntity.getTokenTypeEnum()) {
                        case AUTHORIZATION_CODE:
                            if (result instanceof AuthorizationCodeGrant) {
                                final AuthorizationCode code = new AuthorizationCode(tokenEntity.getTokenCode(), tokenEntity.getCreationDate(), tokenEntity.getExpirationDate());
                                final AuthorizationCodeGrant g = (AuthorizationCodeGrant) result;
                                g.setAuthorizationCode(code);
                            }
                            break;
                        case REFRESH_TOKEN:
                            final RefreshToken refreshToken = new RefreshToken(tokenEntity.getTokenCode(), tokenEntity.getCreationDate(), tokenEntity.getExpirationDate());
                            result.setRefreshTokens(Collections.singletonList(refreshToken));
                            break;
                        case ACCESS_TOKEN:
                            final AccessToken accessToken = new AccessToken(tokenEntity.getTokenCode(), tokenEntity.getCreationDate(), tokenEntity.getExpirationDate());
                            accessToken.setDpop(tokenEntity.getDpop());
                            result.setAccessTokens(Collections.singletonList(accessToken));
                            break;
                        case ID_TOKEN:
                            final IdToken idToken = new IdToken(tokenEntity.getTokenCode(), tokenEntity.getCreationDate(), tokenEntity.getExpirationDate());
                            result.setIdToken(idToken);
                            break;
                        case LONG_LIVED_ACCESS_TOKEN:
                            final AccessToken longLivedAccessToken = new AccessToken(tokenEntity.getTokenCode(), tokenEntity.getCreationDate(), tokenEntity.getExpirationDate());
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
