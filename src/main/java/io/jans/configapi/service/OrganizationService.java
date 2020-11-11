package io.jans.configapi.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import io.jans.as.model.configuration.AppConfiguration;

@ApplicationScoped
@Named("organizationService")
public class OrganizationService extends io.jans.as.common.service.OrganizationService {

    @Inject
    ConfigurationService configurationService;

    @Inject
    private AppConfiguration appConfiguration = configurationService.find();

    protected boolean isUseLocalCache() {
        return appConfiguration.getUseLocalCache();
    }
}
