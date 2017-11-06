/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.StaticUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.BatchOperation;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.xdi.ldap.model.SearchScope;
import org.xdi.oxauth.audit.ApplicationAuditLogger;
import org.xdi.oxauth.model.audit.Action;
import org.xdi.oxauth.model.audit.OAuth2AuditLog;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.ClientTokens;
import org.xdi.oxauth.model.common.CacheGrant;
import org.xdi.oxauth.model.common.SessionTokens;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.ldap.Grant;
import org.xdi.oxauth.model.ldap.TokenLdap;
import org.xdi.oxauth.model.ldap.TokenType;
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
 * @version November 11, 2016
 */
@Stateless
@Named
public class GrantService {

    @Inject
    private Logger log;

    @Inject
    private LdapEntryManager ldapEntryManager;

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

    public String buildDn(String p_uniqueIdentifier, String p_grantId, String p_clientId) {
        final StringBuilder dn = new StringBuilder();
        dn.append(String.format("uniqueIdentifier=%s,oxAuthGrantId=%s,", p_uniqueIdentifier, p_grantId));
        dn.append(clientService.buildClientDn(p_clientId));
        return dn.toString();
    }

    public String baseDn() {
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
                    expiration = Integer.toString(appConfiguration.getAccessTokenLifetime());
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

        prepareGrantBranch(token.getGrantId(), token.getClientId());
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

    public void remove(Grant grant) {
        ldapEntryManager.remove(grant);
        log.trace("Removed grant, id: " + grant.getId());
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

    public void removeGrants(List<Grant> entries) {
        if (entries != null && !entries.isEmpty()) {
            for (Grant g : entries) {
                try {
                    remove(g);
                } catch (Exception e) {
                    log.error("Failed to remove entry", e);
                }
            }
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
            return load(baseDn(), p_code);
        }
    }

    private TokenLdap load(String p_baseDn, String p_code) {
        try {
            final List<TokenLdap> entries = ldapEntryManager.findEntries(p_baseDn, TokenLdap.class, Filter.create(String.format("oxAuthTokenCode=%s", TokenHashUtil.getHashedToken(p_code))));
            if (entries != null && !entries.isEmpty()) {
                return entries.get(0);
            }
        } catch (LDAPException e) {
            log.trace(e.getMessage(), e);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
        return null;
    }

    public List<TokenLdap> getGrantsByGrantId(String p_grantId) {
        try {
            return ldapEntryManager.findEntries(baseDn(), TokenLdap.class, Filter.create(String.format("oxAuthGrantId=%s", p_grantId)));
        } catch (LDAPException e) {
            log.trace(e.getMessage(), e);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public List<TokenLdap> getGrantsByAuthorizationCode(String p_authorizationCode) {
        try {
            return ldapEntryManager.findEntries(baseDn(), TokenLdap.class, Filter.create(String.format("oxAuthAuthorizationCode=%s", TokenHashUtil.getHashedToken(p_authorizationCode))));
        } catch (LDAPException e) {
            log.trace(e.getMessage(), e);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public List<TokenLdap> getGrantsBySessionDn(String sessionDn) {
        List<TokenLdap> grants = new ArrayList<TokenLdap>();
        try {
            List<TokenLdap> ldapGrants = ldapEntryManager.findEntries(baseDn(), TokenLdap.class, Filter.create(String.format("oxAuthSessionDn=%s", sessionDn)));
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

    public void cleanUp() {

        // Cleaning oxAuthToken
        BatchOperation<TokenLdap> tokenBatchService = new BatchOperation<TokenLdap>(ldapEntryManager) {
            @Override
            protected List<TokenLdap> getChunkOrNull(int chunkSize) {
                return ldapEntryManager.findEntries(baseDn(), TokenLdap.class, getFilter(), SearchScope.SUB, null, this, 0, chunkSize, chunkSize);
            }

            @Override
            protected void performAction(List<TokenLdap> entries) {
                auditLogging(entries);
                remove(entries);
            }

            private Filter getFilter() {
                try {
                    return Filter.create(String.format("(oxAuthExpiration<=%s)", StaticUtils.encodeGeneralizedTime(new Date())));
                } catch (LDAPException e) {
                    log.trace(e.getMessage(), e);
                    return Filter.createPresenceFilter("oxAuthExpiration");
                }
            }
        };
        tokenBatchService.iterateAllByChunks(CleanerTimer.BATCH_SIZE);

        // Cleaning oxAuthGrant
        BatchOperation<Grant> grantBatchService = new BatchOperation<Grant>(ldapEntryManager) {
            @Override
            protected List<Grant> getChunkOrNull(int chunkSize) {
                return ldapEntryManager.findEntries(baseDn(), Grant.class, getFilter(), SearchScope.SUB, null, this, 0, chunkSize, chunkSize);
            }

            @Override
            protected void performAction(List<Grant> entries) {
                removeGrants(entries);
            }

            private Filter getFilter() {
                try {
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.SECOND, 60);
                    return Filter.create(String.format("(&(oxAuthCreation<=%s)(|(numsubordinates=0)(hasSubordinates=FALSE)))", StaticUtils.encodeGeneralizedTime(calendar.getTime())));
                } catch (LDAPException e) {
                    log.trace(e.getMessage(), e);
                    return Filter.createPresenceFilter("oxAuthCreation");
                }
            }
        };
        grantBatchService.iterateAllByChunks(CleanerTimer.BATCH_SIZE);

        // Cleaning old oxAuthGrant
        // Note: This block should be removed, it is used only to delete old legacy data.
        BatchOperation<Grant> oldGrantBatchService = new BatchOperation<Grant>(ldapEntryManager) {
            @Override
            protected List<Grant> getChunkOrNull(int chunkSize) {
                return ldapEntryManager.findEntries(baseDn(), Grant.class, getFilter(), SearchScope.SUB, null, this, 0, chunkSize, chunkSize);
            }

            @Override
            protected void performAction(List<Grant> entries) {
                removeGrants(entries);
            }

            private Filter getFilter() {
                try {
                    return Filter.create("(&(!(oxAuthCreation=*))(|(numsubordinates=0)(hasSubordinates=FALSE)))");
                } catch (LDAPException e) {
                    log.trace(e.getMessage(), e);
                    return Filter.createPresenceFilter("oxAuthCreation");
                }
            }
        };
        oldGrantBatchService.iterateAllByChunks(CleanerTimer.BATCH_SIZE);
    }

    private void addGrantBranch(final String p_grantId, final String p_clientId) {
        Grant grant = new Grant();
        grant.setDn(getBaseDnForGrant(p_grantId, p_clientId));
        grant.setId(p_grantId);
        grant.setCreationDate(new Date());

        ldapEntryManager.persist(grant);
    }

    private void prepareGrantBranch(final String p_grantId, final String p_clientId) {
        // Create ocAuthGrant branch if needed
        if (!containsGrantBranch(p_grantId, p_clientId)) {
            addGrantBranch(p_grantId, p_clientId);
        }
    }

    private boolean containsGrantBranch(final String p_grantId, final String p_clientId) {
        return ldapEntryManager.contains(Grant.class, getBaseDnForGrant(p_grantId, p_clientId));
    }

    private String getBaseDnForGrant(final String p_grantId, final String p_clientId) {
        final StringBuilder dn = new StringBuilder();
        dn.append(String.format("oxAuthGrantId=%s,", p_grantId));
        dn.append(clientService.buildClientDn(p_clientId));

        return dn.toString();
    }

    private void auditLogging(Collection<TokenLdap> entries) {
        for (TokenLdap tokenLdap : entries) {
            OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(null, Action.SESSION_DESTROYED);
            oAuth2AuditLog.setSuccess(true);
            oAuth2AuditLog.setClientId(tokenLdap.getClientId());
            oAuth2AuditLog.setScope(tokenLdap.getScope());
            oAuth2AuditLog.setUsername(tokenLdap.getUserId());
            applicationAuditLogger.sendMessage(oAuth2AuditLog);
        }
    }
}