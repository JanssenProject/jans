/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */
package io.jans.as.server.ssa.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.service.external.context.ModifySsaResponseContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Stateless
@Named
public class SsaContextBuilder {

    public ModifySsaResponseContext buildModifySsaResponseContext(HttpServletRequest httpRequest, AuthorizationGrant grant,
                                                                  Client client, AppConfiguration appConfiguration, AttributeService attributeService) {
        return new ModifySsaResponseContext(httpRequest, grant, client, appConfiguration, attributeService);
    }
}
