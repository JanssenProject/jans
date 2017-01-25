/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap.persistence.exception;

/**
 * An exception is a result if LDAP entry doesn't require update
 * 
 * @author Yuriy Movchan Date: 01/27/2017
 */
public class EmptyEntryPersistenceException extends EntryPersistenceException {

	private static final long serialVersionUID = 1421766232087075304L;

	public EmptyEntryPersistenceException(Throwable root) {
		super(root);
	}

	public EmptyEntryPersistenceException(String string, Throwable root) {
		super(string, root);
	}

	public EmptyEntryPersistenceException(String s) {
		super(s);
	}

}
