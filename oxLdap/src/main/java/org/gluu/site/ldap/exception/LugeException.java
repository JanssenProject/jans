/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap.exception;

/**
 * Generic LUGE exception
 */
public class LugeException extends Exception {

	/**
	 * S Serialization ID
	 */
	private static final long serialVersionUID = 1518677859552078437L;

	/**
	 * Wrapped LDAP exception
	 */
	Exception e = null;
	/**
	 * LDAP error code
	 */
	int errorCode = 0;

	/**
	 * Constructor with detailed error
	 * 
	 * @param message
	 *            Detailed message
	 */
	public LugeException(final String message) {
		super(message);
	}

	/**
	 * Constructor with error and wrapped exception
	 * 
	 * @param message
	 *            Detailed error
	 * @param e
	 *            Wrapped LDAP exception
	 */
	public LugeException(final String message, final Exception e) {
		super(message);
		this.e = e;
	}

	/**
	 * Get the LDAP error code
	 * 
	 * @return The LDAP error code
	 */
	public int getErrorCode() {
		return errorCode;
	}

	/**
	 * Get the wrapped exception
	 * 
	 * @return The wrapped exception
	 */
	public Exception getException() {
		return e;
	}

	/**
	 * Get the message for display
	 * 
	 * @return Message for display
	 */
	@Override
	public String getMessage() {
		String s = super.getMessage();
		if (e != null) {
			s = s + " --> ";
			s = s + e.getMessage();
		}
		return s;
	}

	/**
	 * Set the LDAP error code
	 * 
	 * @param code
	 *            Error code
	 */
	public void setErrorCode(final int code) {
		errorCode = code;
	}

	/**
	 * Get the message for display
	 * 
	 * @return Message for display
	 */
	@Override
	public String toString() {
		return getMessage();
	}
}
