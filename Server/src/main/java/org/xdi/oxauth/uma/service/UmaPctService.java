package org.xdi.oxauth.uma.service;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.StaticUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.BatchOperation;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.xdi.ldap.model.SearchScope;
import org.xdi.ldap.model.SimpleBranch;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.service.CleanerTimer;
import org.xdi.oxauth.uma.authorization.UmaPCT;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.List;

/**
 * @author yuriyz on 05/31/2017.
 */
@Stateless
@Named
public class UmaPctService {

    @Inject
    private Logger log;

    @Inject
    private LdapEntryManager ldapEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    public UmaPCT getByCode(String pctCode) {
        try {
            final Filter filter = Filter.create(String.format("&(oxAuthTokenCode=%s)", pctCode));
            final String baseDn = staticConfiguration.getBaseDn().getClients();
            final List<UmaPCT> entries = ldapEntryManager.findEntries(baseDn, UmaPCT.class, filter);
            if (entries != null && !entries.isEmpty()) {
                return entries.get(0);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public void save(UmaPCT pct) {
        try {
            prepareBranch();

            pct.setDn(dn(pct.getCode()));
            ldapEntryManager.persist(pct);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void remove(UmaPCT umaPCT) {
        ldapEntryManager.remove(umaPCT);
    }

    public void remove(String pctCode) {
        remove(getByCode(pctCode));
    }

    public void remove(List<UmaPCT> pctList) {
        for (UmaPCT pct : pctList) {
            remove(pct);
        }
    }

    private void prepareBranch() {
        if (!ldapEntryManager.contains(SimpleBranch.class, branchBaseDn())) {
            addBranch();
        }
    }

    public void addBranch() {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName("pct");
        branch.setDn(branchBaseDn());

        ldapEntryManager.persist(branch);
    }

    public String dn(String pctCode) {
        if (StringUtils.isBlank(pctCode)) {
            throw new IllegalArgumentException("PCT code is null or blank.");
        }
        return String.format("oxAuthTokenCode=%s,%s", pctCode, branchBaseDn());
    }

    public String branchBaseDn() {
        final String umaBaseDn = staticConfiguration.getBaseDn().getUmaBase(); // "ou=uma,o=@!1111,o=gluu"
        return String.format("ou=pct,%s", umaBaseDn);
    }

    public void cleanup(final Date now) {
        BatchOperation<UmaPCT> batchService = new BatchOperation<UmaPCT>(ldapEntryManager) {
            @Override
            protected List<UmaPCT> getChunkOrNull(int chunkSize) {
                return ldapEntryManager.findEntries(branchBaseDn(), UmaPCT.class, getFilter(), SearchScope.SUB, null, this, 0, chunkSize, chunkSize);
            }

            @Override
            protected void performAction(List<UmaPCT> entries) {
                for (UmaPCT p : entries) {
                    try {
                        remove(p);
                    } catch (Exception e) {
                        log.error("Failed to remove entry", e);
                    }
                }
            }

            private Filter getFilter() {
                try {
                    return Filter.create(String.format("(oxAuthExpiration<=%s)", StaticUtils.encodeGeneralizedTime(now)));
                }catch (LDAPException e) {
                    log.trace(e.getMessage(), e);
                    return Filter.createPresenceFilter("oxAuthExpiration");
                }
            }
        };
        batchService.iterateAllByChunks(CleanerTimer.BATCH_SIZE);
    }
}
