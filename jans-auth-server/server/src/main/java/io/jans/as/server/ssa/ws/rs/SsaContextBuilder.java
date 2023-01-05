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

/**
 * Provides builder methods for SSA
 */
@Stateless
@Named
public class SsaContextBuilder {

    /**
     * ModifySsaResponseContext instance for use in the SSA custom script call.
     * <p>
     * Method was created with the purpose of passing unit tests, since when instantiating ModifySsaResponseContext
     * it internally call {@link io.jans.service.cdi.util.CdiUtil} and cannot be mocked
     * </p>
     *
     * @param httpRequest      Http request
     * @param grant            Grant type
     * @param client           Client
     * @param appConfiguration App configuration
     * @param attributeService Attribute service
     * @return New instance of {@link ModifySsaResponseContext}
     */
    @Deprecated
    public ModifySsaResponseContext buildModifySsaResponseContext(HttpServletRequest httpRequest, AuthorizationGrant grant,
                                                                  Client client, AppConfiguration appConfiguration, AttributeService attributeService) {
        return new ModifySsaResponseContext(httpRequest, grant, client, appConfiguration, attributeService);
    }
}
