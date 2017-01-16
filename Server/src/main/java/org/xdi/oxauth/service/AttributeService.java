/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import java.util.List;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.model.config.StaticConf;
import org.xdi.service.CacheService;
import org.xdi.util.StringHelper;

/**
 * @author Javier Rojas Blum
 * @version 0.9 March 27, 2015
 */
@Scope(ScopeType.STATELESS)
@Name("attributeService")
@AutoCreate
public class AttributeService extends org.xdi.service.AttributeService {

    private static final String CACHE_ATTRIBUTE = "AttributeCache";

    @Logger
    private Log log;

    @In
    private CacheService cacheService;

    @In
    private StaticConf staticConfiguration;
    /**
     * Get AttributeService instance
     *
     * @return AttributeService instance
     */
    public static AttributeService instance() {
        return (AttributeService) Component.getInstance(AttributeService.class);
    }

    /**
     * returns GluuAttribute by Dn
     *
     * @return GluuAttribute
     */
    public GluuAttribute getAttributeByDn(String dn) {
        GluuAttribute gluuAttribute = (GluuAttribute) cacheService.get(CACHE_ATTRIBUTE, dn);

        if (gluuAttribute == null) {
            gluuAttribute = ldapEntryManager.find(GluuAttribute.class, dn);
            cacheService.put(CACHE_ATTRIBUTE, dn, gluuAttribute);
        } else {
            log.trace("Get attribute from cache by Dn '{0}'", dn);
        }

        return gluuAttribute;
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

}