/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client.uma.exception;

/**
 * UMA Exception
 * 
 * @author Yuriy Movchan Date: 12/08/2012
 */
public class UmaException extends Exception {

	private static final long serialVersionUID = 2136659058534678566L;

	public UmaException() {}

	public UmaException(String message) {}

	public UmaException(Throwable cause) {
		super(cause);
	}

	public UmaException(String message, Throwable cause) {
		super(message, cause);
	}

}
