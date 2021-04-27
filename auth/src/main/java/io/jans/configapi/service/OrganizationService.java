package io.jans.configapi.service;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
public class OrganizationService extends io.jans.as.common.service.OrganizationService {
    @Override
    protected boolean isUseLocalCache() {
        return false;
    }
}