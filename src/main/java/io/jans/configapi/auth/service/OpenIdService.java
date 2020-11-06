/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth.service;

import io.jans.configapi.auth.AuthClientFactory;
import io.jans.configapi.auth.client.OpenIdClientService;
import io.jans.configapi.service.ConfigurationService;
import io.jans.util.exception.ConfigurationException;
import io.jans.util.init.Initializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.Serializable;
import org.slf4j.Logger;

@ApplicationScoped
public class OpenIdService extends Initializable implements Serializable {

    private static final long serialVersionUID = 4564959567069741194L;

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    private OpenIdClientService introspectionService;

    public OpenIdClientService getIntrospectionService() {
        init();
        return introspectionService;
    }

    @Override
    protected void initInternal() {
        try {
            loadOpenIdConfiguration();
        } catch (IOException ex) {
            log.error("Failed to load oxAuth OpenId configuration", ex);
            throw new ConfigurationException("Failed to load oxAuth OpenId configuration", ex);
        }
    }

    private void loadOpenIdConfiguration() throws IOException {
        String introspectionEndpoint = configurationService.find().getIntrospectionEndpoint();
        this.introspectionService = AuthClientFactory.getIntrospectionService(introspectionEndpoint, false);
    }

}
