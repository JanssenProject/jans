/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap.persistence.exception;

/**
 * An exception is a result if user don't have required permissions.
 * 
 * @author Yuriy Movchan Date: 10.26.2010
 */
public class AuthenticationException extends LdapMappingException {

	private static final long serialVersionUID = -3321766232087075304L;

	public AuthenticationException(Throwable root) {
		super(root);
	}

	public AuthenticationException(String string, Throwable root) {
		super(string, root);
	}

	public AuthenticationException(String s) {
		super(s);
	}

}
