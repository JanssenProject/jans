/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.model.GluuAttribute;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.service.BaseCacheService;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

/**
 * @author Javier Rojas Blum
 * @version May 30, 2018
 */
@ApplicationScoped
public class AttributeService extends org.gluu.service.AttributeService {

    /**
	 * 
	 */
	private static final long serialVersionUID = -990409035168814270L;

    @Inject
    private Logger log;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
	private AppConfiguration appConfiguration;

    /**
     * returns GluuAttribute by Dn
     *
     * @return GluuAttribute
     */
    public GluuAttribute getAttributeByDn(String dn) {
    	BaseCacheService usedCacheService = getCacheService();

    	return usedCacheService.getWithPut(dn, () -> persistenceEntryManager.find(GluuAttribute.class, dn), 60);
    }

    public GluuAttribute getByLdapName(String name) {
        List<GluuAttribute> gluuAttributes = getAttributesByAttribute("gluuAttributeName", name, staticConfiguration.getBaseDn().getAttributes());
        if (gluuAttributes.size() > 0) {
            for (GluuAttribute gluuAttribute : gluuAttributes) {
                if (gluuAttribute.getName() != null && gluuAttribute.getName().equals(name)) {
                    return gluuAttribute;
                }
            }
        }

        return null;
    }

    public GluuAttribute getByClaimName(String name) {
        List<GluuAttribute> gluuAttributes = getAttributesByAttribute("oxAuthClaimName", name, staticConfiguration.getBaseDn().getAttributes());
        if (gluuAttributes.size() > 0) {
            for (GluuAttribute gluuAttribute : gluuAttributes) {
                if (gluuAttribute.getOxAuthClaimName() != null && gluuAttribute.getOxAuthClaimName().equals(name)) {
                    return gluuAttribute;
                }
            }
        }

        return null;
    }

    public List<GluuAttribute> getAllAttributes() {
        return getAllAttributes(staticConfiguration.getBaseDn().getAttributes());
    }

    public String getDnForAttribute(String inum) {
        String attributesDn = staticConfiguration.getBaseDn().getAttributes();
        if (StringHelper.isEmpty(inum)) {
            return attributesDn;
        }

        return String.format("inum=%s,%s", inum, attributesDn);
    }

    public List<String> getAttributesDn(List<String> claimNames) {
        List<String> claims = new ArrayList<String>();

        for (String claimName : claimNames) {
            GluuAttribute gluuAttribute = getByClaimName(claimName);
            if (gluuAttribute != null) {
                claims.add(gluuAttribute.getDn());
            }
        }

        return claims;
    }

    protected BaseCacheService getCacheService() {
    	if (appConfiguration.getUseLocalCache()) {
    		return localCacheService;
    	}
    	
    	return cacheService;
    }

}