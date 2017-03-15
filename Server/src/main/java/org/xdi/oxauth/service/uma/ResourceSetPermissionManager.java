/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.site.ldap.persistence.BatchOperation;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.xdi.ldap.model.SearchScope;
import org.xdi.ldap.model.SimpleBranch;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.service.CleanerTimer;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.StaticUtils;

/**
 * Holds resource set permission tokens and permissions
 *
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/02/2013
 */
@Stateless
@Named
public class ResourceSetPermissionManager extends AbstractResourceSetPermissionManager {

    private static final String ORGUNIT_OF_RESOURCE_SET_PERMISSION = "uma_resource_set_permission";

    @Inject
    private Logger log;

    @Inject
    private LdapEntryManager ldapEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    public static String getDn(String clientDn, String ticket) {
        return String.format("oxTicket=%s,%s", ticket, getBranchDn(clientDn));
    }

    public static String getBranchDn(String clientDn) {
        return String.format("ou=%s,%s", ORGUNIT_OF_RESOURCE_SET_PERMISSION, clientDn);
    }

    @Override
    public void addResourceSetPermission(ResourceSetPermission resourceSetPermission, String clientDn) {
        try {
            addBranchIfNeeded(clientDn);
            resourceSetPermission.setDn(getDn(clientDn, resourceSetPermission.getTicket()));
            ldapEntryManager.persist(resourceSetPermission);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
    }

    @Override
    public ResourceSetPermission getResourceSetPermissionByTicket(String p_ticket) {
        try {
            final String baseDn = staticConfiguration.getBaseDn().getClients();
            final Filter filter = Filter.create(String.format("&(oxTicket=%s)", p_ticket));
            final List<ResourceSetPermission> entries = ldapEntryManager.findEntries(baseDn, ResourceSetPermission.class, filter);
            if (entries != null && !entries.isEmpty()) {
                return entries.get(0);
            }
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String getResourceSetPermissionTicketByConfigurationCode(String configurationCode, String clientDn) {
        final ResourceSetPermission permission = getResourceSetPermissionByConfigurationCode(configurationCode, clientDn);
        if (permission != null) {
            return permission.getTicket();
        }
        return null;
    }

    public ResourceSetPermission getResourceSetPermissionByConfigurationCode(String p_configurationCode, String clientDn) {
        try {
            final Filter filter = Filter.create(String.format("&(oxConfigurationCode=%s)", p_configurationCode));
            final List<ResourceSetPermission> entries = ldapEntryManager.findEntries(clientDn, ResourceSetPermission.class, filter);
            if (entries != null && !entries.isEmpty()) {
                return entries.get(0);
            }
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void deleteResourceSetPermission(String p_ticket) {
        try {
            final ResourceSetPermission permission = getResourceSetPermissionByTicket(p_ticket);
            if (permission != null) {
                ldapEntryManager.remove(permission);
            }
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
    }

    @Override
    public void cleanupResourceSetPermissions(final Date now) {
        BatchOperation<ResourceSetPermission> resourceSetPermissionBatchService = new BatchOperation<ResourceSetPermission>(ldapEntryManager) {
            @Override
            protected List<ResourceSetPermission> getChunkOrNull(int chunkSize) {
                return ldapEntryManager.findEntries(staticConfiguration.getBaseDn().getClients(), ResourceSetPermission.class, getFilter(), SearchScope.SUB, null, this, 0, chunkSize, chunkSize);
            }

            @Override
            protected void performAction(List<ResourceSetPermission> entries) {
                for (ResourceSetPermission p : entries) {
                    try {
                        ldapEntryManager.remove(p);
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
        resourceSetPermissionBatchService.iterateAllByChunks(CleanerTimer.BATCH_SIZE);
    }

    public void addBranch(String clientDn) {
        final SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName(ORGUNIT_OF_RESOURCE_SET_PERMISSION);
        branch.setDn(getBranchDn(clientDn));
        ldapEntryManager.persist(branch);
    }

    public void addBranchIfNeeded(String clientDn) {
        if (!containsBranch(clientDn)) {
            addBranch(clientDn);
        }
    }

    public boolean containsBranch(String clientDn) {
        return ldapEntryManager.contains(SimpleBranch.class, getBranchDn(clientDn));
    }
}
