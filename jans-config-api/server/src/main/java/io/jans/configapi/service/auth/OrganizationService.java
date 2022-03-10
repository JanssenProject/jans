package io.jans.configapi.service.auth;

import jakarta.enterprise.context.ApplicationScoped;
import io.jans.model.ApplicationType;

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