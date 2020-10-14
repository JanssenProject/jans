/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.exception;

/**
 * @author Yuriy Movchan
 * @version 0.1, 08/21/2013
 */
public class UmaProtectionException extends Exception {

	private static final long serialVersionUID = 2148886143372789053L;

	public UmaProtectionException(String message) {
		super(message);
	}

	public UmaProtectionException(String message, Throwable cause) {
        super(message, cause);
    }

}
