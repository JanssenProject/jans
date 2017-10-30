/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap.persistence.exception;

/**
 * Configuration exception
 */
public class InvalidConfigurationException extends RuntimeException {

	private static final long serialVersionUID = 6541769232087073304L;

	public InvalidConfigurationException(Throwable root) {
		super(root);
	}

	public InvalidConfigurationException(String string, Throwable root) {
		super(string, root);
	}

	public InvalidConfigurationException(String s) {
		super(s);
	}

}
