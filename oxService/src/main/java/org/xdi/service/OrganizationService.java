/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 *//**
 * 
 */
package org.xdi.service;

import java.io.Serializable;

import javax.inject.Inject;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;

/**
 * @author "Oleksiy Tataryn"
 *
 */
public abstract class OrganizationService implements Serializable {

	private static final long serialVersionUID = -6601700282123372943L;

    @Inject
	protected Logger log;

    @Inject
	protected LdapEntryManager ldapEntryManager;
	
	public String getDnForOrganization(String inum, String baseDn) {
		if( baseDn == null ){
			baseDn = "o=gluu";
		}
		return String.format("o=%s,%s", inum, baseDn);
	}

}
