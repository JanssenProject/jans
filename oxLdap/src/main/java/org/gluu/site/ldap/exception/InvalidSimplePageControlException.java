/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap.exception;

/**
 * Invalid page control LDAP exception -- thrown when Simple Page Control returns result without cookie
 * 
 * @author Yuriy Movchan Date: 12/30/2016
 */
public class InvalidSimplePageControlException extends LugeException {

	private static final long serialVersionUID = 1756816743469359856L;

	public InvalidSimplePageControlException(String message) {
		super(message);
	}

}
