/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import java.util.Date;

import org.xdi.oxauth.model.uma.persistence.UmaPermission;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/02/2013
 */

public interface IResourceSetPermissionManager {

    public void addResourceSetPermission(UmaPermission resourceSetPermission, String clientDn);

    public UmaPermission getResourceSetPermissionByTicket(String resourceSetPermissionTicket);

    public String getResourceSetPermissionTicketByConfigurationCode(String configurationCode, String clientDn);

    public UmaPermission createResourceSetPermission(String amHost, org.xdi.oxauth.model.uma.UmaPermission resourceSetPermissionRequest, Date expirationDate);

    public void deleteResourceSetPermission(String resourceSetPermissionTicket);

    public void cleanupResourceSetPermissions(Date now);
}
