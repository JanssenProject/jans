/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.oxauth.persistence.model.configuration;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * Provides global inum search ability.
 * @author Oleksiy Tataryn
 *
 */
@LdapEntry
@LdapObjectClass(values = { "top" })
public class InumEntry extends Entry {

	@LdapAttribute(ignoreDuringUpdate = true)
	private String inum;

	/**
	 * @param inum the inum to set
	 */
	public void setInum(String inum) {
		this.inum = inum;
	}


	/**
	 * @return the inum
	 */
	public String getInum() {
		return inum;
	}


	@Override
	public String toString() {
		return String.format("Entry [dn=%s, inum=%s]", getDn(), getInum());
	}


}
