/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import java.util.Date;
import java.util.UUID;

import javax.inject.Inject;

import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.util.INumGenerator;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/02/2013
 */

public abstract class UmaAbstractPermissionManager implements IPermissionManager {
	
	@Inject
	private ScopeService scopeService;

    public UmaPermission createPermission(String amHost, org.xdi.oxauth.model.uma.UmaPermission permissionRequest, Date expirationDate) {
        final String ticket = UUID.randomUUID().toString();
        final String configurationCode = INumGenerator.generate(8) + "." + System.currentTimeMillis();
        return new UmaPermission(permissionRequest.getResourceId(), scopeService.getScopeDNsByUrlsAndAddToLdapIfNeeded(permissionRequest.getScopes()),
                amHost, "", ticket, configurationCode, expirationDate);
    }

}
