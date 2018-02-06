/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.exception.mapping;

/**
 * The base {@link Throwable} type for LDAP Mapping.
 */
public class BaseMappingException extends RuntimeException {

	private static final long serialVersionUID = 1071769232087073304L;

	public BaseMappingException(Throwable root) {
		super(root);
	}

	public BaseMappingException(String string, Throwable root) {
		super(string, root);
	}

	public BaseMappingException(String s) {
		super(s);
	}

}
