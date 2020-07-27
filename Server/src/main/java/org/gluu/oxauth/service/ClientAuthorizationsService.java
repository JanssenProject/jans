/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import org.apache.commons.lang3.ArrayUtils;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.ldap.ClientAuthorization;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Javier Rojas Blum
 * @version March 4, 2020
 */
@Named
public class ClientAuthorizationsService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private ClientService clientService;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

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
        String baseDn = createDn(null);
        if (!ldapEntryManager.hasBranchesSupport(baseDn)) {
        	return;
        }

        // Create client authorizations branch if needed
        if (!containsBranch()) {
            addBranch();
        }
    }

    public ClientAuthorization find(String userInum, String clientId) {
        prepareBranch();

        try {
            if (appConfiguration.getClientAuthorizationBackwardCompatibility()) {
                return findToRemoveIn50(userInum, clientId);
            }
            return ldapEntryManager.find(ClientAuthorization.class, createDn(createId(userInum, clientId)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    // old version should should be removed in 5.0 version. (We have to fetch entry by key instead of query to improve performance)
    public ClientAuthorization findToRemoveIn50(String userInum, String clientId) {
        Filter filter = Filter.createANDFilter(
                Filter.createEqualityFilter("oxAuthClientId", clientId),
                Filter.createEqualityFilter("oxAuthUserId", userInum)
        );

        List<ClientAuthorization> entries = ldapEntryManager.findEntries(staticConfiguration.getBaseDn().getAuthorizations(), ClientAuthorization.class, filter);
        if (entries != null && !entries.isEmpty()) {
            if (entries.size() > 1) {
                for (ClientAuthorization entry : entries) {
                    if (entry.getId().equals(createId(entry.getUserId(), entry.getClientId()))) {
                        return entry; // return entry where id fits to "userId + _ + clientId" pattern
                    }
                }
            }
            return entries.get(0);
        }

        return null;
    }

    public void clearAuthorizations(ClientAuthorization clientAuthorization, boolean persistInPersistence) {
        if (clientAuthorization == null) {
            return;
        }

        if (persistInPersistence) {
            ldapEntryManager.remove(clientAuthorization);
        }
    }

    public void add(String userInum, String clientId, Set<String> scopes) {
        log.trace("Attempting to add client authorization, scopes:" + scopes + ", clientId: " + clientId + ", userInum: " + userInum);
        Client client = clientService.getClient(clientId);


        // oxAuth #441 Pre-Authorization + Persist Authorizations... don't write anything
        // If a client has pre-authorization=true, there is no point to create the entry under
        // ou=clientAuthorizations it will negatively impact performance, grow the size of the
        // ldap database, and serve no purpose.
        prepareBranch();

        ClientAuthorization clientAuthorization = find(userInum, clientId);

        if (clientAuthorization == null) {
            final String id = createId(userInum, clientId);

            clientAuthorization = new ClientAuthorization();
            clientAuthorization.setId(id);
            clientAuthorization.setDn(createDn(id));
            clientAuthorization.setClientId(clientId);
            clientAuthorization.setUserId(userInum);
            clientAuthorization.setScopes(scopes.toArray(new String[scopes.size()]));
            clientAuthorization.setDeletable(!client.getAttributes().getKeepClientAuthorizationAfterExpiration());
            clientAuthorization.setExpirationDate(client.getExpirationDate());
            clientAuthorization.setTtl(appConfiguration.getDynamicRegistrationExpirationTime());

            ldapEntryManager.persist(clientAuthorization);
        } else if (ArrayUtils.isNotEmpty(clientAuthorization.getScopes())) {
            Set<String> set = new HashSet<>(scopes);
            set.addAll(Arrays.asList(clientAuthorization.getScopes()));

            if (set.size() != scopes.size()) {
                clientAuthorization.setScopes(set.toArray(new String[set.size()]));
                ldapEntryManager.merge(clientAuthorization);
            }
        }
    }

    public static String createId(String userId, String clientId) {
        return userId + "_" + clientId;
    }

    public String createDn(String oxId) {
        String baseDn = staticConfiguration.getBaseDn().getAuthorizations();
        if (StringHelper.isEmpty(oxId)) {
            return baseDn;
        }
        return String.format("oxId=%s,%s", oxId, baseDn);
    }
}
