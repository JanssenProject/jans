/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap.exception;

/**
 * LDAP entry not found exception
 * 
 * @author Pankaj
 */
public class EntryNotFoundException extends LugeException {
	/**
	 * Serialization ID
	 */
	private static final long serialVersionUID = -1429068470601742118L;

	/**
	 * Default constructor
	 */
	public EntryNotFoundException() {
		super("Entry not found");
	}

	/**
	 * Constructor to return the offending DN
	 * 
	 * @param dn
	 *            DN that was not found
	 */
	public EntryNotFoundException(final String dn) {
		super("Entry not found: " + dn);
	}
}
