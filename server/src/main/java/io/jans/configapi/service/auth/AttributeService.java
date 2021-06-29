
package io.jans.configapi.service.auth;

import javax.enterprise.context.ApplicationScoped;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class AttributeService extends io.jans.as.common.service.AttributeService {

    @Override
    protected boolean isUseLocalCache() {
        return false;
    }
}