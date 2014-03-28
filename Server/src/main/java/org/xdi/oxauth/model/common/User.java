package org.xdi.oxauth.model.common;

import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Yuriy Movchan Date: 06/11/2013
 */
@LdapEntry
@LdapObjectClass(values = {"top", "gluuPerson"})
public class User extends SimpleUser {

    private static final long serialVersionUID = 6634191420188575733L;

}