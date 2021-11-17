/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.service;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.service.common.InumService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.configapi.rest.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class ClientService implements Serializable {

    private static final long serialVersionUID = 7912416439116338984L;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private Logger logger;
    
    @Inject
    private OrgService orgService;


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
        String orgDn = orgService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=clients,%s", orgDn);
        }
        return String.format("inum=%s,ou=clients,%s", inum, orgDn);
    }

   
}
