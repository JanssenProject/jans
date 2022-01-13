/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.exception;

public class DuplicateEmailException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8140817165913364968L;

	public DuplicateEmailException(String message) {
		super(message);
	}

	@Override
	public String getMessage() {
		return "Email already used!";
	}

}
