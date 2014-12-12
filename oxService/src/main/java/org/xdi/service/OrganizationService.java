/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 *//**
 * 
 */
package org.xdi.service;

import java.io.Serializable;

import lombok.Data;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.log.Log;

/**
 * @author "Oleksiy Tataryn"
 *
 */
public abstract @Data class OrganizationService implements Serializable {

	private static final long serialVersionUID = -6601700282123372943L;

	@Logger
	private Log log;

	@In
	private LdapEntryManager ldapEntryManager;
	
	public String getDnForOrganization(String inum, String baseDn) {
		if( baseDn == null ){
			baseDn = "o=gluu";
		}
		return String.format("o=%s,%s", inum, baseDn);
	}

}
