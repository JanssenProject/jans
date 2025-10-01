/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.exception;

/**
 * RuntimeException Class for Fido2CompromisedDevice 
 * Extends RuntimeException
 *
 */
public class Fido2CompromisedDevice extends RuntimeException {

    private static final long serialVersionUID = -318563205092295773L;

	/**
	 * Constructor for Fido2CompromisedDevice
	 * @param message String: the detailed message
	 * @param cause Throwable: the cause
	 */
	public Fido2CompromisedDevice(String message, Throwable cause) {
		super(message, cause);
	} 

	public Fido2CompromisedDevice(String message) {
		super(message);
	}

	public Fido2CompromisedDevice(Throwable cause) {
		super(cause);
	}

}
