/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap.exception;

/**
 * Exception thrown when a connection problem occurs
 * 
 * @author Pankaj
 */
public class ConnectionException extends LugeException {
	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = 3976926204785744888L;

	/**
	 * Default constructor
	 */
	public ConnectionException() {
		super("Connection exception");
	}

	/**
	 * Wrap an exception
	 * 
	 * @param e
	 *            Underlying exception
	 */
	public ConnectionException(final Exception e) {
		super("Connection exception: " + e.getMessage());
	}

	/**
	 * Wrap a message
	 * 
	 * @param msg
	 *            Message
	 */
	public ConnectionException(final String msg) {
		super("Connection exception (" + msg + ")");
	}

	/**
	 * Wrap a message and exception
	 * 
	 * @param msg
	 *            Message
	 * @param e
	 *            Exception
	 */
	public ConnectionException(final String msg, final Exception e) {
		this(msg + ": " + e.getMessage());
	}
}
