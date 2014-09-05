/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;

/**
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/02/2013
 */

public class ResourceSetPermissionManagerInMemory extends AbstractResourceSetPermissionManager implements Serializable {

    private static final long serialVersionUID = -3993184173686369535L;

    private ConcurrentHashMap<String, ResourceSetPermission> resourceSetPermissions = new ConcurrentHashMap<String, ResourceSetPermission>();
    private ConcurrentHashMap<String, String> ticketToConfigurationCodeMapping = new ConcurrentHashMap<String, String>();

    public void addResourceSetPermission(ResourceSetPermission resourceSetPermission, String clientDn) {
        String resourceSetPermissionTicket = resourceSetPermission.getTicket();
        this.resourceSetPermissions.put(resourceSetPermissionTicket, resourceSetPermission);
        this.ticketToConfigurationCodeMapping.put(resourceSetPermission.getConfigurationCode(), resourceSetPermissionTicket);
    }

    public ResourceSetPermission getResourceSetPermissionByTicket(String resourceSetPermissionTicket) {
        return resourceSetPermissions.get(resourceSetPermissionTicket);
    }

    public String getResourceSetPermissionTicketByConfigurationCode(String configurationCode, String clientDn) {
        return ticketToConfigurationCodeMapping.get(configurationCode);
    }

    public void deleteResourceSetPermission(String resourceSetPermissionTicket) {
        if (!this.resourceSetPermissions.containsKey(resourceSetPermissionTicket)) {
            return;
        }

        ResourceSetPermission resourceSetPermission = this.resourceSetPermissions.get(resourceSetPermissionTicket);

        this.resourceSetPermissions.remove(resourceSetPermissionTicket);
        this.ticketToConfigurationCodeMapping.remove(resourceSetPermission.getConfigurationCode());
    }

    public void cleanupResourceSetPermissions(Date now) {
        for (Iterator<Map.Entry<String, ResourceSetPermission>> it = this.resourceSetPermissions.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, ResourceSetPermission> resourceSetPermissionEntry = (Map.Entry<String, ResourceSetPermission>) it.next();
            ResourceSetPermission resourceSetPermission = resourceSetPermissionEntry.getValue();

            resourceSetPermission.checkExpired(now);
            if (!resourceSetPermission.isValid()) {
                it.remove();
                this.ticketToConfigurationCodeMapping.remove(resourceSetPermission.getConfigurationCode());
            }
        }
    }
}
