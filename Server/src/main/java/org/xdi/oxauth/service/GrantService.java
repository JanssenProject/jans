/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;
import org.xdi.oxauth.audit.ApplicationAuditLogger;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.CacheGrant;
import org.xdi.oxauth.model.common.ClientTokens;
import org.xdi.oxauth.model.common.SessionTokens;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.ldap.TokenLdap;
import org.xdi.oxauth.model.ldap.TokenType;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.util.TokenHashUtil;
import org.xdi.service.CacheService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

import static org.xdi.oxauth.util.ServerUtil.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version November 28, 2018
 */
@Stateless
@Named
public class GrantService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private ClientService clientService;

    @Inject
    private CacheService cacheService;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

    public static String generateGrantId() {
        return UUID.randomUUID().toString();
    }

    public String buildDn(String p_uniqueIdentifier, String p_clientId) {
        return String.format("uniqueIdentifier=%s,", p_uniqueIdentifier) + tokenBaseDn(p_clientId);
    }

    public String clientsBaseDn() {
        return staticConfiguration.getBaseDn().getClients();  // ou=clients,o=@!1111,o=gluu
    }

    public void merge(TokenLdap p_token) {
        ldapEntryManager.merge(p_token);
    }

    public void mergeSilently(TokenLdap p_token) {
        try {
            ldapEntryManager.merge(p_token);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
    }

    private boolean shouldPutInCache(TokenType tokenType, boolean isImplicitFlow) {
        if (isImplicitFlow && BooleanUtils.isTrue(appConfiguration.getUseCacheForAllImplicitFlowObjects())) {
            return true;
        }

        switch (tokenType) {
            case ID_TOKEN:
                if (!isTrue(appConfiguration.getPersistIdTokenInLdap())) {
                    return true;
                }
            case REFRESH_TOKEN:
                if (!isTrue(appConfiguration.getPersistRefreshTokenInLdap())) {
                    return true;
                }
        }
        return false;
    }

    public void persist(TokenLdap token) {
        String hashedToken = TokenHashUtil.getHashedToken(token.getTokenCode());
        token.setTokenCode(hashedToken);

        if (shouldPutInCache(token.getTokenTypeEnum(), token.isImplicitFlow())) {
            ClientTokens clientTokens = getCacheClientTokens(token.getClientId());
            clientTokens.getTokenHashes().add(hashedToken);

            String expiration = null;
            switch (token.getTokenTypeEnum()) {
                case ID_TOKEN:
                    expiration = Integer.toString(appConfiguration.getIdTokenLifetime());
                    break;
                case REFRESH_TOKEN:
                    expiration = Integer.toString(appConfiguration.getRefreshTokenLifetime());
                    break;
                case ACCESS_TOKEN:
                    int lifetime = appConfiguration.getAccessTokenLifetime();
                    Client client = clientService.getClient(token.getClientId());
                    // oxAuth #830 Client-specific access token expiration
                    if (client != null && client.getAccessTokenLifetime() != null && client.getAccessTokenLifetime() > 0) {
                        lifetime = client.getAccessTokenLifetime();
                    }
                    expiration = Integer.toString(lifetime);
                    break;
            }

            token.setIsFromCache(true);
            cacheService.put(expiration, hashedToken, token);
            cacheService.put(expiration, clientTokens.cacheKey(), clientTokens);

            if (StringUtils.isNotBlank(token.getSessionDn())) {
                SessionTokens sessionTokens = getCacheSessionTokens(token.getSessionDn());
                sessionTokens.getTokenHashes().add(hashedToken);

                cacheService.put(expiration, sessionTokens.cacheKey(), sessionTokens);
            }
            return;
        }

        prepareBranch(token.getClientId());

        ldapEntryManager.persist(token);
    }

    public ClientTokens getCacheClientTokens(String clientId) {
        ClientTokens clientTokens = new ClientTokens(clientId);
        Object o = cacheService.get(null, clientTokens.cacheKey());
        if (o instanceof ClientTokens) {
            return (ClientTokens) o;
        } else {
            return clientTokens;
        }
    }

    public SessionTokens getCacheSessionTokens(String sessionDn) {
        SessionTokens sessionTokens = new SessionTokens(sessionDn);
        Object o = cacheService.get(null, sessionTokens.cacheKey());
        if (o instanceof SessionTokens) {
            return (SessionTokens) o;
        } else {
            return sessionTokens;
        }
    }

    public void remove(TokenLdap p_token) {
        if (p_token.isFromCache()) {
            cacheService.remove(null, TokenHashUtil.getHashedToken(p_token.getTokenCode()));
            log.trace("Removed token from cache, code: " + p_token.getTokenCode());
        } else {
            ldapEntryManager.remove(p_token);
            log.trace("Removed token from LDAP, code: " + p_token.getTokenCode());
        }
    }

    public void removeSilently(TokenLdap token) {
        try {
            remove(token);

            if (StringUtils.isNotBlank(token.getAuthorizationCode())) {
                cacheService.remove(null, CacheGrant.cacheKey(token.getClientId(), token.getAuthorizationCode(), token.getGrantId()));
            }
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
    }

    public void remove(List<TokenLdap> p_entries) {
        if (p_entries != null && !p_entries.isEmpty()) {
            for (TokenLdap t : p_entries) {
                try {
                    remove(t);
                } catch (Exception e) {
                    log.error("Failed to remove entry", e);
                }
            }
        }
    }

    public void removeSilently(List<TokenLdap> p_entries) {
        if (p_entries != null && !p_entries.isEmpty()) {
            for (TokenLdap t : p_entries) {
                removeSilently(t);
            }
        }
    }

    public void remove(AuthorizationGrant p_grant) {
        if (p_grant != null && p_grant.getTokenLdap() != null) {
            try {
                remove(p_grant.getTokenLdap());
            } catch (Exception e) {
                log.trace(e.getMessage(), e);
            }
        }
    }

    public List<TokenLdap> getGrantsOfClient(String p_clientId) {
        try {
            final String baseDn = clientService.buildClientDn(p_clientId);
            return ldapEntryManager.findEntries(baseDn, TokenLdap.class, Filter.create("oxAuthTokenCode=*"));
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public TokenLdap getGrantsByCodeAndClient(String p_code, String p_clientId) {
        return load(clientService.buildClientDn(p_clientId), p_code);
    }

    public TokenLdap getGrantsByCode(String p_code) {
        return getGrantsByCode(p_code, false);
    }


    public TokenLdap getGrantsByCode(String p_code, boolean onlyFromCache) {
        Object grant = cacheService.get(null, TokenHashUtil.getHashedToken(p_code));
        if (grant instanceof TokenLdap) {
            return (TokenLdap) grant;
        } else {
            if (onlyFromCache) {
                return null;
            }
            return load(clientsBaseDn(), p_code);
        }
    }

    private TokenLdap load(String p_baseDn, String p_code) {
        try {
            final List<TokenLdap> entries = ldapEntryManager.findEntries(p_baseDn, TokenLdap.class, Filter.createEqualityFilter("oxAuthTokenCode", TokenHashUtil.getHashedToken(p_code)));
            if (entries != null && !entries.isEmpty()) {
                return entries.get(0);
            }
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
        return null;
    }

    public List<TokenLdap> getGrantsByGrantId(String p_grantId) {
        try {
            return ldapEntryManager.findEntries(clientsBaseDn(), TokenLdap.class, Filter.createEqualityFilter("oxAuthGrantId", p_grantId));
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public List<TokenLdap> getGrantsByAuthorizationCode(String p_authorizationCode) {
        try {
            return ldapEntryManager.findEntries(clientsBaseDn(), TokenLdap.class, Filter.createEqualityFilter("oxAuthAuthorizationCode", TokenHashUtil.getHashedToken(p_authorizationCode)));
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public List<TokenLdap> getGrantsBySessionDn(String sessionDn) {
        List<TokenLdap> grants = new ArrayList<TokenLdap>();
        try {
            List<TokenLdap> ldapGrants = ldapEntryManager.findEntries(clientsBaseDn(), TokenLdap.class, Filter.create(String.format("oxAuthSessionDn=%s", sessionDn)));
            if (ldapGrants != null) {
                grants.addAll(ldapGrants);
            }
            grants.addAll(getGrantsFromCacheBySessionDn(sessionDn));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return grants;
    }

    public List<TokenLdap> getGrantsFromCacheBySessionDn(String sessionDn) {
        if (StringUtils.isBlank(sessionDn)) {
            return Collections.emptyList();
        }
        return getCacheTokensEntries(getCacheSessionTokens(sessionDn).getTokenHashes());
    }

    public List<TokenLdap> getCacheClientTokensEntries(String clientId) {
        Object o = cacheService.get(null, new ClientTokens(clientId).cacheKey());
        if (o instanceof ClientTokens) {
            return getCacheTokensEntries(((ClientTokens) o).getTokenHashes());
        }
        return Collections.emptyList();
    }

    public List<TokenLdap> getCacheTokensEntries(Set<String> tokenHashes) {
        List<TokenLdap> tokens = new ArrayList<TokenLdap>();

        for (String tokenHash : tokenHashes) {
            Object o1 = cacheService.get(null, tokenHash);
            if (o1 instanceof TokenLdap) {
                TokenLdap token = (TokenLdap) o1;
                token.setIsFromCache(true);
                tokens.add(token);
            }
        }
        return tokens;
    }

    public void removeAllTokensBySession(String sessionDn) {
        removeSilently(getGrantsBySessionDn(sessionDn));
    }

    /**
     * Removes grant with particular code.
     *
     * @param p_code code
     */
    public void removeByCode(String p_code, String p_clientId) {
        final TokenLdap t = getGrantsByCodeAndClient(p_code, p_clientId);
        if (t != null) {
            removeSilently(t);
        }
        cacheService.remove(null, CacheGrant.cacheKey(p_clientId, p_code, null));
    }

    public void removeAllByAuthorizationCode(String p_authorizationCode) {
        removeSilently(getGrantsByAuthorizationCode(p_authorizationCode));
    }

    public void removeAllByGrantId(String p_grantId) {
        removeSilently(getGrantsByGrantId(p_grantId));
    }

    private void prepareBranch(final String clientId) {
        if (!ldapEntryManager.contains(SimpleBranch.class, tokenBaseDn(clientId))) {
            SimpleBranch branch = new SimpleBranch();
            branch.setOrganizationalUnitName("token");
            branch.setDn(tokenBaseDn(clientId));

            ldapEntryManager.persist(branch);
        }
    }

    private String tokenBaseDn(final String clientId) {
        return "ou=token," + clientService.buildClientDn(clientId);
    }
}