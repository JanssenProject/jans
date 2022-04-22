/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.service;

import io.jans.as.common.model.registration.Client;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.StringHelper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

import org.slf4j.Logger;

@ApplicationScoped
@Named("cltSrv")
public class ClientService implements Serializable {

    private static final long serialVersionUID = 7912416439116338984L;

    @Inject
    private transient PersistenceEntryManager persistenceEntryManager;

    @Inject
    private transient Logger logger;

    public boolean contains(String clientDn) {
        return persistenceEntryManager.contains(clientDn, Client.class);
    }

    public Client getClientByInum(String inum) {
        Client result = null;
        try {
            result = persistenceEntryManager.find(Client.class, getDnForClient(inum));
        } catch (Exception ex) {
            logger.error("Failed to load client entry", ex);
        }
        return result;
    }

    public Client getClientByDn(String dn) {
        try {
            return persistenceEntryManager.find(Client.class, dn);
        } catch (Exception e) {
            logger.warn("", e);
            return null;
        }
    }

    public String getDnForClient(String inum) {
        String orgDn = getDnForOrganization(null);
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=clients,%s", orgDn);
        }
        return String.format("inum=%s,ou=clients,%s", inum, orgDn);
    }

    public String getDnForOrganization(String baseDn) {
        if (baseDn == null) {
            baseDn = "o=jans";
        }
        return baseDn;
    }

}
