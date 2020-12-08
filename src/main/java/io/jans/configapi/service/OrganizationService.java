package io.jans.configapi.service;

import javax.enterprise.context.ApplicationScoped;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class OrganizationService extends io.jans.as.common.service.OrganizationService {
    @Override
    protected boolean isUseLocalCache() {
        return false;
    }
}
