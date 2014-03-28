package org.xdi.oxauth.service;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.GluuAttribute;

/**
 * @author Javier Rojas Blum Date: 07.10.2012
 */
@Scope(ScopeType.STATELESS)
@Name("attributeService")
@AutoCreate
public class AttributeService {

    @In
    LdapEntryManager ldapEntryManager;

    @Logger
    private Log log;

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
        return ldapEntryManager.find(GluuAttribute.class, Dn);
    }
}