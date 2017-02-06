/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */
package org.xdi.config.oxtrust;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapJsonObject;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * Shibboleth IDP CAS-related settings configuration entry. 
 * 
 * @author Dmitry Ognyannikov
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxShibbolethCASProtocolConfiguration"})
public class LdapShibbolethCASProtocolConfiguration {

    private static final long serialVersionUID = -11887457695212971L;
    
    @LdapJsonObject
    @LdapAttribute(name = "oxConfApplication")
    private ShibbolethCASProtocolConfiguration casProtocolConfiguration;

    @LdapAttribute(name = "oxRevision")
    private long revision;
    
    public LdapShibbolethCASProtocolConfiguration() {}

    /**
     * @return the casProtocolConfiguration
     */
    public ShibbolethCASProtocolConfiguration getCasProtocolConfiguration() {
        return casProtocolConfiguration;
    }

    /**
     * @param casProtocolConfiguration the casProtocolConfiguration to set
     */
    public void setCasProtocolConfiguration(ShibbolethCASProtocolConfiguration casProtocolConfiguration) {
        this.casProtocolConfiguration = casProtocolConfiguration;
    }

    /**
     * @return the revision
     */
    public long getRevision() {
        return revision;
    }

    /**
     * @param revision the revision to set
     */
    public void setRevision(long revision) {
        this.revision = revision;
    }
    
}
