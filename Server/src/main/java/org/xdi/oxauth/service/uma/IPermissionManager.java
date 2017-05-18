/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import org.xdi.oxauth.model.uma.persistence.UmaPermission;

import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/02/2013
 */

public interface IPermissionManager {

    void addPermission(UmaPermission permission, String clientDn);

    UmaPermission getPermissionByTicket(String ticket);

    String getPermissionTicketByConfigurationCode(String configurationCode, String clientDn);

    UmaPermission createPermission(String amHost, org.xdi.oxauth.model.uma.UmaPermission permissionRequest, Date expirationDate);

    void deletePermission(String ticket);

    void cleanupPermissions(Date now);
}
