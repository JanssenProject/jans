/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.exception.fido.u2f;

public class InvalidKeyHandleDeviceException extends Exception {

	private static final long serialVersionUID = 4324358428668365475L;

	public InvalidKeyHandleDeviceException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidKeyHandleDeviceException(String message) {
		super(message);
	}

}
