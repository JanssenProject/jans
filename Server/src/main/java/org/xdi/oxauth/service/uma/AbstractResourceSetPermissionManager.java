/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import org.xdi.oxauth.model.uma.UmaPermission;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.util.INumGenerator;

import java.util.Date;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/02/2013
 */

public abstract class AbstractResourceSetPermissionManager implements IResourceSetPermissionManager {

    public ResourceSetPermission createResourceSetPermission(String amHost, UmaPermission p_request, Date expirationDate) {
        final String ticket = UUID.randomUUID().toString();
        final String configurationCode = INumGenerator.generate(8) + "." + System.currentTimeMillis();
        final ScopeService scopeService = ScopeService.instance();
        return new ResourceSetPermission(p_request.getResourceSetId(), scopeService.getScopeDNsByUrlsAndAddToLdapIfNeeded(p_request.getScopes()),
                amHost, "", ticket, configurationCode, expirationDate);
    }
}
