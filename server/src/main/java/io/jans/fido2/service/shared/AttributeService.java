/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.shared;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.oxauth.model.config.StaticConfiguration;
import io.jans.service.BaseCacheService;
import io.jans.util.StringHelper;

/**
 * @author Yuriy Movchan
 * @version May 20, 2020
 */
@ApplicationScoped
public class AttributeService extends org.gluu.service.AttributeService {

	private static final long serialVersionUID = -990409035168814270L;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
	private AppConfiguration appConfiguration;


    public String getDnForAttribute(String inum) {
        String attributesDn = staticConfiguration.getBaseDn().getAttributes();
        if (StringHelper.isEmpty(inum)) {
            return attributesDn;
        }

        return String.format("inum=%s,%s", inum, attributesDn);
    }

    protected BaseCacheService getCacheService() {
    	if (appConfiguration.isUseLocalCache()) {
    		return localCacheService;
    	}
    	
    	return cacheService;
    }

}