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
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.base.SimpleBranch;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.HashSet;
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

        final String id = createId(userInum, clientId);
        try {
            return ldapEntryManager.find(ClientAuthorization.class, createDn(id));
        } catch (EntryPersistenceException e) {
            log.trace("Unable to find client persistence for {}", id);
            return null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
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

            if (set.size() != clientAuthorization.getScopes().length) {
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
