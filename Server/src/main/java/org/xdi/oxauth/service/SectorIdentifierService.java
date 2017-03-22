package org.xdi.oxauth.service;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.ldap.SectorIdentifier;
import org.xdi.util.StringHelper;

/**
 * @author Javier Rojas Blum
 * @version January 15, 2016
 */
@Stateless
@Named
public class SectorIdentifierService {

    @Inject
    private Logger log;

    @Inject
    private LdapEntryManager ldapEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    /**
     * Get sector identifier by inum
     *
     * @param inum Sector identifier inum
     * @return Sector identifier
     */
    public SectorIdentifier getSectorIdentifierByInum(String inum) {
        SectorIdentifier result = null;
        try {
            result = ldapEntryManager.find(SectorIdentifier.class, getDnForSectorIdentifier(inum));
        } catch (Exception e) {
            log.error("Failed to find sector identifier by Inum " + inum, e);
        }
        return result;
    }

    /**
     * Build DN string for sector identifier
     *
     * @param inum Sector Identifier Inum
     * @return DN string for specified sector identifier or DN for sector identifiers branch if inum is null
     * @throws Exception
     */
    public String getDnForSectorIdentifier(String inum) {
        String sectorIdentifierDn = staticConfiguration.getBaseDn().getSectorIdentifiers();
        if (StringHelper.isEmpty(inum)) {
            return sectorIdentifierDn;
        }

        return String.format("inum=%s,%s", inum, sectorIdentifierDn);
    }
}
