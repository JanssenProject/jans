/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.model.config.ConfigurationFactory;

import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version 0.9 February 12, 2015
 */
@Scope(ScopeType.STATELESS)
@Name("attributeService")
@AutoCreate
public class AttributeService extends org.xdi.service.AttributeService {

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
    public GluuAttribute getScopeByDn(String dn) {
        return getLdapEntryManager().find(GluuAttribute.class, dn);
    }

    public GluuAttribute getByLdapName(String name) {
        List<GluuAttribute> gluuAttributes = getAttributesByAttribute("gluuLdapAttributeName", name, ConfigurationFactory.getBaseDn().getAttributes());
        if (gluuAttributes.size() > 0) {
            for (GluuAttribute gluuAttribute : gluuAttributes) {
                if (gluuAttribute.getGluuLdapAttributeName() != null && gluuAttribute.getGluuLdapAttributeName().equals(name)) {
                    return gluuAttribute;
                }
            }
        }

        return null;
    }

    public GluuAttribute getByClaimName(String name) {
        List<GluuAttribute> gluuAttributes = getAttributesByAttribute("oxAuthClaimName", name, ConfigurationFactory.getBaseDn().getAttributes());
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
        return getAllAttributes(ConfigurationFactory.getBaseDn().getAttributes());
    }
}