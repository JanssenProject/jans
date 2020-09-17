package org.gluu.oxauth.service.common;

import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.service.OrganizationService;
import org.gluu.oxauth.util.OxConstants;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.gluu.util.StringHelper;
import org.oxauth.persistence.model.SectorIdentifier;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * @author gasmyr on 9/17/20.
 */
@ApplicationScoped
public class SectorService implements Serializable {
    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private ClientService clientService;


    public String getDnForSectorIdentifier(String oxId) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(oxId)) {
            return String.format("ou=sector_identifiers,%s", orgDn);
        }

        return String.format("oxId=%s,ou=sector_identifiers,%s", oxId, orgDn);
    }

    public List<SectorIdentifier> searchSectorIdentifiers(String pattern, int sizeLimit) {
        String[] targetArray = new String[]{pattern};
        Filter searchFilter = Filter.createSubstringFilter(OxConstants.oxId, null, targetArray, null);

        List<SectorIdentifier> result = persistenceEntryManager.findEntries(getDnForSectorIdentifier(null),
                SectorIdentifier.class, searchFilter, sizeLimit);

        return result;
    }

    public List<SectorIdentifier> getAllSectorIdentifiers() {
        return persistenceEntryManager.findEntries(getDnForSectorIdentifier(null), SectorIdentifier.class, null);
    }


    public SectorIdentifier getSectorIdentifierById(String oxId) {
        try {
            return persistenceEntryManager.find(SectorIdentifier.class, getDnForSectorIdentifier(oxId));
        } catch (Exception e) {
            log.warn("Failed to find sector identifier by oxId " + oxId, e);
            return null;
        }
    }


    public String generateIdForNewSectorIdentifier() {
        SectorIdentifier sectorIdentifier = new SectorIdentifier();
        String newId = null;
        String newDn = null;
        do {
            newId = generateIdForNewSectorIdentifierImpl();
            newDn = getDnForSectorIdentifier(newId);
            sectorIdentifier.setDn(newDn);
        } while (persistenceEntryManager.contains(newDn, SectorIdentifier.class));
        return newId;
    }


    private String generateIdForNewSectorIdentifierImpl() {
        return UUID.randomUUID().toString();
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
