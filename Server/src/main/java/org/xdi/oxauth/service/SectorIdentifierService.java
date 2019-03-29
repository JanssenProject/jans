package org.xdi.oxauth.service;

import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.persist.PersistenceEntryManager;
import org.gluu.util.StringHelper;
import org.oxauth.persistence.model.SectorIdentifier;
import org.slf4j.Logger;
import org.xdi.oxauth.model.config.StaticConfiguration;

/**
 * @author Javier Rojas Blum
 * @version January 15, 2016
 */
@Named
public class SectorIdentifierService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    /**
     * Get sector identifier by oxId
     *
     * @param oxId Sector identifier oxId
     * @return Sector identifier
     */
    public SectorIdentifier getSectorIdentifierById(String oxId) {
        SectorIdentifier result = null;
        try {
            result = ldapEntryManager.find(SectorIdentifier.class, getDnForSectorIdentifier(oxId));
        } catch (Exception e) {
            log.error("Failed to find sector identifier by oxId " + oxId, e);
        }
        return result;
    }

    /**
     * Build DN string for sector identifier
     *
     * @param oxId Sector Identifier oxId
     * @return DN string for specified sector identifier or DN for sector identifiers branch if oxId is null
     * @throws Exception
     */
    public String getDnForSectorIdentifier(String oxId) {
        String sectorIdentifierDn = staticConfiguration.getBaseDn().getSectorIdentifiers();
        if (StringHelper.isEmpty(oxId)) {
            return sectorIdentifierDn;
        }

        return String.format("oxId=%s,%s", oxId, sectorIdentifierDn);
    }
}
