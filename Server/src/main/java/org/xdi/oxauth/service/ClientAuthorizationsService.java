/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.gluu.service.CacheService;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.xdi.oxauth.model.ldap.ClientAuthorizations;

/**
 * @author Javier Rojas Blum
 * @version January 17, 2018
 */
@Stateless
@Named
public class ClientAuthorizationsService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private UserService userService;

    @Inject
    private CacheService cacheService;

    private static final String CACHE_CLIENT_CUTHORIZATION = "ClientAuthorizationCache";

    public void addBranch(final String userInum) {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName("clientAuthorizations");
        branch.setDn(getBaseDnForClientAuthorizations(userInum));

        ldapEntryManager.persist(branch);
    }

    public boolean containsBranch(final String userInum) {
        return ldapEntryManager.contains(SimpleBranch.class, getBaseDnForClientAuthorizations(userInum));
    }

    public void prepareBranch(final String userInum) {
        // Create client authorizations branch if needed
        if (!containsBranch(userInum)) {
            addBranch(userInum);
        }
    }

    public ClientAuthorizations findClientAuthorizations(String userInum, String clientId, boolean persistInLdap) {
        if (persistInLdap) {
            prepareBranch(userInum);

            String baseDn = getBaseDnForClientAuthorizations(userInum);
            Filter filter = Filter.createEqualityFilter("oxAuthClientId", clientId);

            List<ClientAuthorizations> entries = ldapEntryManager.findEntries(baseDn, ClientAuthorizations.class, filter);
            if (entries != null && !entries.isEmpty()) {
                // if more then one entry then it's problem, non-deterministic behavior, id must be unique
                if (entries.size() > 1) {
                    log.error("Found more then one client authorization entry by client Id: {}" + clientId);
                    for (ClientAuthorizations entry : entries) {
                        log.error(entry.toString());
                    }
                }
                return entries.get(0);
            }
        } else {
            String key = getCacheKey(userInum, clientId);
            Object cacheOjb = cacheService.get(CACHE_CLIENT_CUTHORIZATION, key);
            if (cacheOjb != null && cacheOjb instanceof ClientAuthorizations) {
                return (ClientAuthorizations) cacheOjb;
            }
        }

        return null;
    }

    public void add(String userInum, String clientId, Set<String> scopes, boolean persistInLdap) {
        if (persistInLdap) {
            // oxAuth #441 Pre-Authorization + Persist Authorizations... don't write anything
            // If a client has pre-authorization=true, there is no point to create the entry under
            // ou=clientAuthorizations it will negatively impact performance, grow the size of the
            // ldap database, and serve no purpose.
            prepareBranch(userInum);

            ClientAuthorizations clientAuthorizations = findClientAuthorizations(userInum, clientId, persistInLdap);

            if (clientAuthorizations == null) {
                clientAuthorizations = new ClientAuthorizations();
                clientAuthorizations.setId(UUID.randomUUID().toString());
                clientAuthorizations.setClientId(clientId);
                clientAuthorizations.setScopes(scopes.toArray(new String[scopes.size()]));
                clientAuthorizations.setDn(getBaseDnForClientAuthorizations(clientAuthorizations.getId(), userInum));

                ldapEntryManager.persist(clientAuthorizations);
            } else if (clientAuthorizations.getScopes() != null) {
                Set<String> set = new HashSet<String>(scopes);
                set.addAll(Arrays.asList(clientAuthorizations.getScopes()));
                clientAuthorizations.setScopes(set.toArray(new String[set.size()]));

                ldapEntryManager.merge(clientAuthorizations);
            }
        } else {
            // Put client authorization in cache. oxAuth #662.
            ClientAuthorizations clientAuthorizations = findClientAuthorizations(userInum, clientId, persistInLdap);
            String key = getCacheKey(userInum, clientId);

            if (clientAuthorizations == null) {
                clientAuthorizations = new ClientAuthorizations();
                clientAuthorizations.setId(UUID.randomUUID().toString());
                clientAuthorizations.setClientId(clientId);
                clientAuthorizations.setScopes(scopes.toArray(new String[scopes.size()]));
                clientAuthorizations.setDn(getBaseDnForClientAuthorizations(clientAuthorizations.getId(), userInum));

                cacheService.put(CACHE_CLIENT_CUTHORIZATION, key, clientAuthorizations);
            } else if (clientAuthorizations.getScopes() != null) {
                Set<String> set = new HashSet<String>(scopes);
                set.addAll(Arrays.asList(clientAuthorizations.getScopes()));
                clientAuthorizations.setScopes(set.toArray(new String[set.size()]));

                cacheService.put(CACHE_CLIENT_CUTHORIZATION, key, clientAuthorizations);
            }
        }
    }

    public String getBaseDnForClientAuthorizations(String oxId, String userInum) {
        String baseDn = getBaseDnForClientAuthorizations(userInum);
        if (StringHelper.isEmpty(oxId)) {
            return baseDn;
        }
        return String.format("oxId=%s,%s", oxId, baseDn);
    }

    public String getBaseDnForClientAuthorizations(String userInum) {
        final String userBaseDn = userService.getDnForUser(userInum); // inum=1234,ou=people,o=gluu"
        return String.format("ou=clientAuthorizations,%s", userBaseDn); // "ou=clientAuthorizations,inum=1234,ou=people,o=gluu"
    }

    private String getCacheKey(String userInum, String clientId) {
        return userInum + "_" + clientId;
    }
}
