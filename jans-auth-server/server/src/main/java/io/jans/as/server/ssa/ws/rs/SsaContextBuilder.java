/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */
package io.jans.as.server.ssa.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.external.context.ModifySsaResponseContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Provides builder methods for SSA
 */
@Stateless
@Named
public class SsaContextBuilder {

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private AttributeService attributeService;

    /**
     * ModifySsaResponseContext instance for use in the SSA custom script call.
     * <p>
     * Method was created with the purpose of passing unit tests, since when instantiating ModifySsaResponseContext
     * it internally call {@link io.jans.service.cdi.util.CdiUtil} and cannot be mocked
     * </p>
     *
     * @param httpRequest Http request
     * @param client      Client
     * @return New instance of {@link ModifySsaResponseContext}
     */
    @Deprecated
    public ModifySsaResponseContext buildModifySsaResponseContext(HttpServletRequest httpRequest, Client client) {
        return new ModifySsaResponseContext(httpRequest, null, client, appConfiguration, attributeService);
    }
}
