/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.log.Log;
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.service.CacheService;

import java.util.List;

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

    /**
     * Get AttributeService instance
     *
     * @return AttributeService instance
     */
    public static AttributeService instance() {
        boolean createContexts = !Contexts.isEventContextActive() && !Contexts.isApplicationContextActive();
        if (createContexts) {
            Lifecycle.beginCall();
        }

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
        List<GluuAttribute> gluuAttributes = getAttributesByAttribute("gluuAttributeName", name, ConfigurationFactory.instance().getBaseDn().getAttributes());
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
        List<GluuAttribute> gluuAttributes = getAttributesByAttribute("oxAuthClaimName", name, ConfigurationFactory.instance().getBaseDn().getAttributes());
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
        return getAllAttributes(ConfigurationFactory.instance().getBaseDn().getAttributes());
    }
}