/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.fido2.service.shared;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.fido2.model.conf.AppConfiguration;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.service.BaseCacheService;
import org.gluu.util.StringHelper;

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