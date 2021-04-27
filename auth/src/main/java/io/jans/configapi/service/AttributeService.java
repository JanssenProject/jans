
package io.jans.configapi.service;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
public class AttributeService extends io.jans.as.common.service.AttributeService {

    @Override
    protected boolean isUseLocalCache() {
        return false;
    }
}