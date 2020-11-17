/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth.service;

import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.configapi.auth.client.AuthClientFactory;
import io.jans.configapi.auth.client.OpenIdClientService;
import io.jans.configapi.service.ConfigurationService;
import io.jans.util.exception.ConfigurationException;
import io.jans.util.init.Initializable;
import org.slf4j.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.Serializable;

@ApplicationScoped
public class OpenIdService extends Initializable implements Serializable {

    private static final long serialVersionUID = 4564959567069741194L;

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    private IntrospectionService introspectionService;

    public IntrospectionService getIntrospectionService() {
        init();
        return introspectionService;
    }

    public String getIntrospectionEndpoint() {
        return configurationService.find().getIntrospectionEndpoint();
    }

    public IntrospectionResponse getIntrospectionResponse(String header, String token) {
        return AuthClientFactory.getIntrospectionResponse(getIntrospectionEndpoint(), header, token, false);
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
        log.debug("OpenIdService::loadOpenIdConfiguration() - configurationService.find().getIntrospectionEndpoint() = "
                + configurationService.find().getIntrospectionEndpoint());
        String introspectionEndpoint = configurationService.find().getIntrospectionEndpoint();
        this.introspectionService = AuthClientFactory.getIntrospectionService(introspectionEndpoint, false);

        log.debug("\n\n OpenIdService::loadOpenIdConfiguration() - introspectionService =" + introspectionService);
        log.info("Successfully loaded oxAuth configuration");
    }

}
