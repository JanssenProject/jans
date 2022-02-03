package io.jans.configapi.service.auth;

import io.jans.as.client.service.OrgConfigurationService;
import io.jans.configapi.security.client.AuthClientFactory;
import io.jans.model.ApplicationType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class OrganizationService extends io.jans.as.common.service.OrganizationService {
    
    @Inject
    Logger log;
    
    @Inject
    AuthClientFactory authClientFactory;
    
    @Override
    protected boolean isUseLocalCache() {
        return false;
    }

    public ApplicationType getApplicationType() {
        return ApplicationType.JANS_CONFIG_API;
    }
    
    public OrgConfigurationService getOrgConfigurationService(String url) {
        return AuthClientFactory.getOrgConfigService(url);
    }
}