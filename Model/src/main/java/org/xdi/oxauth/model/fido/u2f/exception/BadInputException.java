/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.xdi.oxauth.model.fido.u2f.exception;

/**
 * @author Yuriy Movchan Date: 05/13/2015
 */
public class BadInputException extends RuntimeException {

	private static final long serialVersionUID = -2738024707341148557L;

	public BadInputException(String message) {
		super(message);
	}

	public BadInputException(String message, Throwable cause) {
		super(message, cause);
	}
}
