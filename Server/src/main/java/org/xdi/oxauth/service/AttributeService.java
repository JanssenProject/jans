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
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.model.config.ConfigurationFactory;

/**
 * @author Javier Rojas Blum Date: 07.10.2012
 */
@Scope(ScopeType.STATELESS)
@Name("attributeService")
@AutoCreate
public class AttributeService extends org.xdi.service.AttributeService{


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
    public GluuAttribute getScopeByDn(String Dn) {
        return getLdapEntryManager().find(GluuAttribute.class, Dn);
    }

    
    public List<GluuAttribute> getAllAttributes(){
    	return getAllAttributes(ConfigurationFactory.getBaseDn().getAttributes());
    }


}