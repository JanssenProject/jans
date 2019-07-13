/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.ldap.ClientAuthorizations;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.gluu.service.CacheService;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

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
    private CacheService cacheService;

    @Inject
    private ClientService clientService;

    @Inject
    private StaticConfiguration staticConfiguration;

    public void addBranch() {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName("authorizations");
        branch.setDn(createDn(null));

        ldapEntryManager.persist(branch);
    }

    public boolean containsBranch() {
        return ldapEntryManager.contains(createDn(null), SimpleBranch.class);
    }

    public void prepareBranch() {
        // Create client authorizations branch if needed
        if (!containsBranch()) {
            addBranch();
        }
    }

    public ClientAuthorizations findClientAuthorizations(String userInum, String clientId, boolean persistInLdap) {
        if (persistInLdap) {
            prepareBranch();

            Filter filter = Filter.createANDFilter(
                    Filter.createEqualityFilter("oxAuthClientId", clientId),
                    Filter.createEqualityFilter("oxAuthUserId", userInum)
            );

            List<ClientAuthorizations> entries = ldapEntryManager.findEntries(staticConfiguration.getBaseDn().getAuthorizations(), ClientAuthorizations.class, filter);
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
            Object cacheOjb = cacheService.get(key);
            if (cacheOjb != null && cacheOjb instanceof ClientAuthorizations) {
                return (ClientAuthorizations) cacheOjb;
            }
        }

        return null;
    }

    public void add(String userInum, String clientId, Set<String> scopes, boolean persistInLdap) {
        Client client = clientService.getClient(clientId);

        if (persistInLdap) {
            // oxAuth #441 Pre-Authorization + Persist Authorizations... don't write anything
            // If a client has pre-authorization=true, there is no point to create the entry under
            // ou=clientAuthorizations it will negatively impact performance, grow the size of the
            // ldap database, and serve no purpose.
            prepareBranch();

            ClientAuthorizations clientAuthorizations = findClientAuthorizations(userInum, clientId, persistInLdap);

            if (clientAuthorizations == null) {
                clientAuthorizations = new ClientAuthorizations();
                clientAuthorizations.setId(UUID.randomUUID().toString());
                clientAuthorizations.setClientId(clientId);
                clientAuthorizations.setUserId(userInum);
                clientAuthorizations.setScopes(scopes.toArray(new String[scopes.size()]));
                clientAuthorizations.setDn(createDn(clientAuthorizations.getId()));
                clientAuthorizations.setDeletable(!client.getAttributes().getKeepClientAuthorizationAfterExpiration());
                clientAuthorizations.setExpirationDate(client.getExpirationDate());

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
                clientAuthorizations.setUserId(userInum);
                clientAuthorizations.setScopes(scopes.toArray(new String[scopes.size()]));
                clientAuthorizations.setDn(createDn(clientAuthorizations.getId()));
                clientAuthorizations.setDeletable(!client.getAttributes().getKeepClientAuthorizationAfterExpiration());
                clientAuthorizations.setExpirationDate(client.getExpirationDate());

                cacheService.put(key, clientAuthorizations);
            } else if (clientAuthorizations.getScopes() != null) {
                Set<String> set = new HashSet<String>(scopes);
                set.addAll(Arrays.asList(clientAuthorizations.getScopes()));
                clientAuthorizations.setScopes(set.toArray(new String[set.size()]));

                cacheService.put(key, clientAuthorizations);
            }
        }
    }

    public String createDn(String oxId) {
        String baseDn = staticConfiguration.getBaseDn().getAuthorizations();
        if (StringHelper.isEmpty(oxId)) {
            return baseDn;
        }
        return String.format("oxId=%s,%s", oxId, baseDn);
    }

    private String getCacheKey(String userInum, String clientId) {
        return userInum + "_" + clientId;
    }
}
