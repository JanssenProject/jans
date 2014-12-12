/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.service.ldap;

import java.util.Properties;

import org.gluu.site.ldap.LDAPConnectionProvider;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

/**
 * Super class to forbid interceptor calls
 * 
 * @author Yuriy Movchan Date: 08/09/2013
 */
@BypassInterceptors
public class LdapConnectionService extends LDAPConnectionProvider {

	public LdapConnectionService(Properties props) {
		super(props);
	}

}
