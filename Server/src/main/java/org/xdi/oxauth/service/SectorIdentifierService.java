package org.xdi.oxauth.service;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.StaticConf;
import org.xdi.oxauth.model.ldap.SectorIdentifier;
import org.xdi.util.StringHelper;

/**
 * @author Javier Rojas Blum
 * @version January 15, 2016
 */
@Scope(ScopeType.STATELESS)
@Name("sectorIdentifierService")
@AutoCreate
public class SectorIdentifierService {

    @Logger
    private Log log;
    @In
    private LdapEntryManager ldapEntryManager;

    @In
    private StaticConf staticConfiguration;

    public static SectorIdentifierService instance() {
        return (SectorIdentifierService) Component.getInstance(SectorIdentifierService.class);
    }

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
