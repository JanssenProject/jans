/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.util.AttributeConstants;
import io.jans.as.persistence.model.SectorIdentifier;
import io.jans.configapi.service.OrganizationService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.List;

/**
 * @author Mougang T.Gasmyr
 */
@ApplicationScoped
public class SectorService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private ClientService clientService;

    public String getDnForSectorIdentifier(String jsId) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(jsId)) {
            return String.format("ou=sector_identifiers,%s", orgDn);
        }
        return String.format("jansId=%s,ou=sector_identifiers,%s", jsId, orgDn);
    }

    public List<SectorIdentifier> searchSectorIdentifiers(String pattern, int sizeLimit) {
        String[] targetArray = new String[]{pattern};
        Filter searchFilter = Filter.createSubstringFilter(AttributeConstants.jsId, null, targetArray, null);
        return persistenceEntryManager.findEntries(getDnForSectorIdentifier(null), SectorIdentifier.class, searchFilter,
                sizeLimit);
    }

    public List<SectorIdentifier> getAllSectorIdentifiers() {
        return persistenceEntryManager.findEntries(getDnForSectorIdentifier(null), SectorIdentifier.class, null);
    }

    public SectorIdentifier getSectorIdentifierById(String jsId) {
        try {
            return persistenceEntryManager.find(SectorIdentifier.class, getDnForSectorIdentifier(jsId));
        } catch (Exception e) {
            log.trace("Failed to find sector identifier by jsId " + jsId, e);
            return null;
        }
    }

    public void addSectorIdentifier(SectorIdentifier sectorIdentifier) {
        persistenceEntryManager.persist(sectorIdentifier);
    }

    public void updateSectorIdentifier(SectorIdentifier sectorIdentifier) {
        persistenceEntryManager.merge(sectorIdentifier);
    }

    public void removeSectorIdentifier(SectorIdentifier sectorIdentifier) {
        if (sectorIdentifier.getClientIds() != null) {
            List<String> clientDNs = sectorIdentifier.getClientIds();
            for (String clientDN : clientDNs) {
                Client client = clientService.getClientByDn(clientDN);
                client.setSectorIdentifierUri(null);
                clientService.updateClient(client);
            }
        }
        persistenceEntryManager.remove(sectorIdentifier);
    }

}
