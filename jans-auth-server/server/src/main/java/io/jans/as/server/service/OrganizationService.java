/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.model.ApplicationType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

@ApplicationScoped
@Named("organizationService")
public class OrganizationService extends io.jans.as.common.service.OrganizationService {

    @Inject
    private AppConfiguration appConfiguration;

    protected boolean isUseLocalCache() {
        return appConfiguration.getUseLocalCache();
    }

    @Override
    public ApplicationType getApplicationType() {
        return ApplicationType.OX_AUTH;
    }

}
