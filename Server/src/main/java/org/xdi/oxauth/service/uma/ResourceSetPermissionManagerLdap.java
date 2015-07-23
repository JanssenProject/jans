/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import java.util.Date;
import java.util.List;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.ldap.model.SimpleBranch;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.util.ServerUtil;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.util.StaticUtils;

/**
 * LDAP version of resource set permission manager.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/02/2013
 */

public class ResourceSetPermissionManagerLdap extends AbstractResourceSetPermissionManager {

    private static final String ORGUNIT_OF_RESOURCE_SET_PERMISSION = "uma_resource_set_permission";

    private static final Log LOG = Logging.getLog(ResourceSetPermissionManagerLdap.class);

    private final LdapEntryManager ldapEntryManager;

    public ResourceSetPermissionManagerLdap() {
        ldapEntryManager = ServerUtil.getLdapManager();
    }

    @Override
    public void addResourceSetPermission(ResourceSetPermission resourceSetPermission, String clientDn) {
        try {
            addBranchIfNeeded(clientDn);
            resourceSetPermission.setDn(getDn(clientDn, resourceSetPermission.getTicket()));
            ldapEntryManager.persist(resourceSetPermission);
        } catch (Exception e) {
            LOG.trace(e.getMessage(), e);
        }
    }

    @Override
    public ResourceSetPermission getResourceSetPermissionByTicket(String p_ticket) {
        try {
            final String baseDn = ConfigurationFactory.instance().getBaseDn().getClients();
            final Filter filter = Filter.create(String.format("&(oxTicket=%s)", p_ticket));
            final List<ResourceSetPermission> entries = ldapEntryManager.findEntries(baseDn, ResourceSetPermission.class, filter);
            if (entries != null && !entries.isEmpty()) {
                return entries.get(0);
            }
        } catch (Exception e) {
            LOG.trace(e.getMessage(), e);
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
            LOG.trace(e.getMessage(), e);
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
            LOG.trace(e.getMessage(), e);
        }
    }

    @Override
    public void cleanupResourceSetPermissions(Date now) {
        try {
            final Filter filter = Filter.create(String.format("(oxAuthExpiration<=%s)", StaticUtils.encodeGeneralizedTime(now)));
            final List<ResourceSetPermission> entries = ldapEntryManager.findEntries(
                    ConfigurationFactory.instance().getBaseDn().getClients(), ResourceSetPermission.class, filter);
            if (entries != null && !entries.isEmpty()) {
                for (ResourceSetPermission p : entries) {
                    ldapEntryManager.remove(p);
                }
            }
        } catch (Exception e) {
            LOG.trace(e.getMessage(), e);
        }
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

    public static String getDn(String clientDn, String ticket) {
        return String.format("oxTicket=%s,%s", ticket, getBranchDn(clientDn));
    }

    public static String getBranchDn(String clientDn) {
        return String.format("ou=%s,%s", ORGUNIT_OF_RESOURCE_SET_PERMISSION, clientDn);
    }
}
