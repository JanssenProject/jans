/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.gluu.oxauth.model.fido.u2f.exception;

/**
 * @author Yuriy Movchan Date: 07/13/2016
 */
public class RegistrationNotAllowed extends RuntimeException {

	private static final long serialVersionUID = -2738024707341148557L;

	public RegistrationNotAllowed(String message) {
		super(message);
	}

	public RegistrationNotAllowed(String message, Throwable cause) {
		super(message, cause);
	}
}
