package io.jans.ca.server.service.auth;

import io.jans.model.ApplicationType;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class OrganizationService extends io.jans.as.common.service.OrganizationService {
    @Override
    protected boolean isUseLocalCache() {
        return false;
    }

    public ApplicationType getApplicationType() {
        return ApplicationType.JANS_CONFIG_API;
    }
}