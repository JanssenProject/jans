/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.CacheGrant;
import io.jans.as.server.model.ldap.TokenLdap;
import io.jans.as.server.model.ldap.TokenType;
import io.jans.as.server.util.TokenHashUtil;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.service.CacheService;
import io.jans.service.cache.CacheConfiguration;

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
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private ClientService clientService;

    @Inject
    private CacheService cacheService;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private CacheConfiguration cacheConfiguration;

    public static String generateGrantId() {
        return UUID.randomUUID().toString();
    }

    public String buildDn(String hashedToken) {
        return String.format("tknCde=%s,", hashedToken) + tokenBaseDn();
    }

    private String tokenBaseDn() {
        return staticConfiguration.getBaseDn().getTokens();  // ou=tokens,o=jans
    }

    public void merge(TokenLdap token) {
        persistenceEntryManager.merge(token);
    }

    public void mergeSilently(TokenLdap token) {
        try {
            persistenceEntryManager.merge(token);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void persist(TokenLdap token) {
        persistenceEntryManager.persist(token);
    }

    public void remove(TokenLdap token) {
        persistenceEntryManager.remove(token);
        log.trace("Removed token from LDAP, code: {}", token.getTokenCode());
    }

    public void removeSilently(TokenLdap token) {
        try {
            remove(token);

            if (StringUtils.isNotBlank(token.getAuthorizationCode())) {
                cacheService.remove(CacheGrant.cacheKey(token.getAuthorizationCode(), token.getGrantId()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void remove(List<TokenLdap> entries) {
        if (entries != null && !entries.isEmpty()) {
            for (TokenLdap t : entries) {
                try {
                    remove(t);
                } catch (Exception e) {
                    log.error("Failed to remove entry", e);
                }
            }
        }
    }

    public void removeSilently(List<TokenLdap> entries) {
        if (entries != null && !entries.isEmpty()) {
            for (TokenLdap t : entries) {
                removeSilently(t);
            }
        }
    }

    public void remove(AuthorizationGrant grant) {
        if (grant != null && grant.getTokenLdap() != null) {
            try {
                remove(grant.getTokenLdap());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public List<TokenLdap> getGrantsOfClient(String clientId) {
        try {
            final String baseDn = clientService.buildClientDn(clientId);
            return persistenceEntryManager.findEntries(baseDn, TokenLdap.class, Filter.createPresenceFilter("tknCde"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public TokenLdap getGrantByCode(String code) {
        Object grant = cacheService.get(TokenHashUtil.hash(code));
        if (grant instanceof TokenLdap) {
            return (TokenLdap) grant;
        } else {
            return load(buildDn(TokenHashUtil.hash(code)));
        }
    }

    private TokenLdap load(String tokenDn) {
        try {
            return persistenceEntryManager.find(TokenLdap.class, tokenDn);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public List<TokenLdap> getGrantsByGrantId(String grantId) {
        try {
            return persistenceEntryManager.findEntries(tokenBaseDn(), TokenLdap.class, Filter.createEqualityFilter("grtId", grantId));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public List<TokenLdap> getGrantsByAuthorizationCode(String authorizationCode) {
        try {
            return persistenceEntryManager.findEntries(tokenBaseDn(), TokenLdap.class, Filter.createEqualityFilter("authzCode", TokenHashUtil.hash(authorizationCode)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public List<TokenLdap> getGrantsBySessionDn(String sessionDn) {
        List<TokenLdap> grants = new ArrayList<>();
        try {
            List<TokenLdap> ldapGrants = persistenceEntryManager.findEntries(tokenBaseDn(), TokenLdap.class, Filter.createEqualityFilter("ssnId", sessionDn));
            if (ldapGrants != null) {
                grants.addAll(ldapGrants);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return grants;
    }

    public void logout(String sessionDn) {
        final List<TokenLdap> tokens = getGrantsBySessionDn(sessionDn);
        if (BooleanUtils.isFalse(appConfiguration.getRemoveRefreshTokensForClientOnLogout())) {
            List<TokenLdap> refreshTokens = Lists.newArrayList();
            for (TokenLdap token : tokens) {
                if (token.getTokenTypeEnum() == TokenType.REFRESH_TOKEN) {
                    refreshTokens.add(token);
                }
            }
            if (!refreshTokens.isEmpty()) {
                log.trace("Refresh tokens are not removed on logout (because removeRefreshTokensForClientOnLogout configuration property is false)");
                tokens.removeAll(refreshTokens);
            }
        }
        removeSilently(tokens);
    }

    public void removeAllTokensBySession(String sessionDn) {
        removeSilently(getGrantsBySessionDn(sessionDn));
    }

    /**
     * Removes grant with particular code.
     *
     * @param code code
     */
    public void removeByCode(String code) {
        final TokenLdap t = getGrantByCode(code);
        if (t != null) {
            removeSilently(t);
        }
        cacheService.remove(CacheGrant.cacheKey(code, null));
    }

    // authorization code is saved only in cache
    public void removeAuthorizationCode(String code) {
        cacheService.remove(CacheGrant.cacheKey(code, null));
    }

    public void removeAllByAuthorizationCode(String authorizationCode) {
        removeSilently(getGrantsByAuthorizationCode(authorizationCode));
    }

    public void removeAllByGrantId(String grantId) {
        removeSilently(getGrantsByGrantId(grantId));
    }

}