/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth.service;

import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.configapi.auth.client.AuthClientFactory;
import io.jans.configapi.service.ConfigurationService;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.inject.Inject;
import java.io.Serializable;

@ApplicationScoped
@Named("openIdService")
public class OpenIdService implements Serializable {

    private static final long serialVersionUID = 4564959567069741194L;

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    private IntrospectionService introspectionService;

    public IntrospectionService getIntrospectionService() {
        return introspectionService;
    }

    public String getIntrospectionEndpoint() {
        return configurationService.find().getIntrospectionEndpoint();
    }

    public IntrospectionResponse getIntrospectionResponse(String header, String token) {
        return AuthClientFactory.getIntrospectionResponse(getIntrospectionEndpoint(), header, token, false);
    }

}
