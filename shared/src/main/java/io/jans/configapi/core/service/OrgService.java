package io.jans.configapi.core.service;

import javax.enterprise.context.ApplicationScoped;
import io.jans.model.ApplicationType;

@ApplicationScoped
public class OrgService extends io.jans.as.common.service.OrganizationService {
    @Override
    protected boolean isUseLocalCache() {
        return false;
    }

    public ApplicationType getApplicationType() {
        return ApplicationType.JANS_CONFIG_API;
    }
}