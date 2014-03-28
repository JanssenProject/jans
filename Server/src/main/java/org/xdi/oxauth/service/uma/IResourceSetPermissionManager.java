package org.xdi.oxauth.service.uma;

import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.model.uma.ResourceSetPermissionRequest;

import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/02/2013
 */

public interface IResourceSetPermissionManager {

    public void addResourceSetPermission(ResourceSetPermission resourceSetPermission, String clientDn);

    public ResourceSetPermission getResourceSetPermissionByTicket(String resourceSetPermissionTicket);

    public String getResourceSetPermissionTicketByConfigurationCode(String configurationCode, String clientDn);

    public ResourceSetPermission createResourceSetPermission(String amHost, String host, ResourceSetPermissionRequest resourceSetPermissionRequest, Date expirationDate);

    public void deleteResourceSetPermission(String resourceSetPermissionTicket);

    public void cleanupResourceSetPermissions(Date now);
}
