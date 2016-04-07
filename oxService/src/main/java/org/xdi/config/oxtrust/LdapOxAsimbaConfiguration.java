/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */
package org.xdi.config.oxtrust;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.ldap.model.Entry;

/**
 * Asimba LDAP settings configuration entry. 
 * 
 * @author Dmitry Ognyannikov
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAsimbaConfiguration"})
public class LdapOxAsimbaConfiguration extends Entry {

    private static final long serialVersionUID = -12489347651302948L;

    @LdapDN
    private String dn;

    @LdapAttribute(name = "oxRevision")
    private long revision;
    
    public LdapOxAsimbaConfiguration() {}
    
}
