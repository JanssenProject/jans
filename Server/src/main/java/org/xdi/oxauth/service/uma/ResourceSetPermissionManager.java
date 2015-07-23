/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.uma.RegisterPermissionRequest;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.util.ServerUtil;

import java.util.Date;

/**
 * Holds resource set permission tokens and permissions
 *
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/02/2013
 */
@AutoCreate
@Scope(ScopeType.APPLICATION)
@Name("resourceSetPermissionManager")
public class ResourceSetPermissionManager implements IResourceSetPermissionManager {

    @Logger
    private Log log;

    private IResourceSetPermissionManager manager;

    @Create
    public void init() {
        switch (ConfigurationFactory.instance().getConfiguration().getModeEnum()) {
            case IN_MEMORY:
                manager = new ResourceSetPermissionManagerInMemory();
                log.info("Created IN-MEMORY UMA resource set manager");
                break;
            case LDAP:
                manager = new ResourceSetPermissionManagerLdap();
                log.info("Created LDAP UMA resource set manager");
                break;
            default:
                log.error("Unable to identify mode of the server. (Please check configuration.)");
                throw new IllegalArgumentException("Unable to identify mode of the server. (Please check configuration.)");
        }
    }

    public void addResourceSetPermission(ResourceSetPermission resourceSetPermission, String p_clientDn) {
        manager.addResourceSetPermission(resourceSetPermission, p_clientDn);
    }

    public ResourceSetPermission getResourceSetPermissionByTicket(String resourceSetPermissionTicket) {
        return manager.getResourceSetPermissionByTicket(resourceSetPermissionTicket);
    }

    public String getResourceSetPermissionTicketByConfigurationCode(String configurationCode, String clientDn) {
        return manager.getResourceSetPermissionTicketByConfigurationCode(configurationCode, clientDn);
    }

    public ResourceSetPermission createResourceSetPermission(String amHost, RegisterPermissionRequest resourceSetPermissionRequest, Date expirationDate) {
        return manager.createResourceSetPermission(amHost, resourceSetPermissionRequest, expirationDate);
    }

    public void deleteResourceSetPermission(String resourceSetPermissionTicket) {
        manager.deleteResourceSetPermission(resourceSetPermissionTicket);
    }

    public void cleanupResourceSetPermissions(Date now) {
        manager.cleanupResourceSetPermissions(now);
    }

    /**
     * Get ResourceSetPermissionManager instance
     *
     * @return ResourceSetPermissionManager instance
     */
    public static ResourceSetPermissionManager instance() {
        return ServerUtil.instance(ResourceSetPermissionManager.class);
    }
}
