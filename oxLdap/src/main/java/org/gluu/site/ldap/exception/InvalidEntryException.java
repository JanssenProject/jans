/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap.exception;

import java.util.List;

/**
 * Invalid LDAP entry exception -- thrown when modifying the directory in an
 * illegal way
 * 
 * @author Pankaj
 */
public class InvalidEntryException extends LugeException {

	/**
	 * Serialization ID
	 */
	private static final long serialVersionUID = 1756816743469359856L;

	/**
	 * Generate the return string based on a list of missing attributes
	 * 
	 * @param missing
	 *            List of missing attributes
	 * @return The string value of all the missing attributes
	 */
	private static String generateMissingString(final List<String> missing) {
		StringBuilder missingString = new StringBuilder();
		for (final String attr : missing) {
            missingString.append(attr).append(" ");
		}
		return missingString.toString();
	}

	/**
	 * A list of missing attributes
	 */
	private List<String> missingAttributes;

	/**
	 * Constructor passing the list of missing attributes
	 * 
	 * @param missing
	 *            Missing attributes
	 */
	public InvalidEntryException(final List<String> missing) {
		super("Invalid entry: The following attributes must be present: " + InvalidEntryException.generateMissingString(missing));
		missingAttributes = missing;
	}

	/**
	 * Constructor for any error
	 * 
	 * @param message
	 *            Error to attach
	 */
	public InvalidEntryException(final String message) {
		super(message);
	}

	/**
	 * Constructor for any error and wrapped exception
	 * 
	 * @param message
	 *            Error to attach
	 * @param e
	 *            LDAPException to wrap
	 */
	public InvalidEntryException(final String message, final Exception e) {
		super(message, e);
	}

	/**
	 * Get the missing attributes
	 * 
	 * @return the missingAttributes
	 */
	public List<String> getMissingAttributes() {
		return missingAttributes;
	}

	/**
	 * @param missingAttributes
	 *            the missingAttributes to set
	 */
	public void setMissingAttributes(final List<String> missingAttributes) {
		this.missingAttributes = missingAttributes;
	}
}
