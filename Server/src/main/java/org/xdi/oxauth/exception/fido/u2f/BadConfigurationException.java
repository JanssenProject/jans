/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.exception.fido.u2f;

public class BadConfigurationException extends RuntimeException {

	private static final long serialVersionUID = -1914683110856700400L;

	public BadConfigurationException(String message) {
		super(message);
	}

	public BadConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
}
