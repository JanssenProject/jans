/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.exception;

/**
 * Indicates that current session should be invalidated
 *
 * @author Yuriy Movchan Date: 06/04//2019
 *
 */
public class InvalidSessionStateException extends RuntimeException {

	private static final long serialVersionUID = -2256375601182225949L;

	public InvalidSessionStateException() {
		super();
	}

	public InvalidSessionStateException(String message) {
		super(message);
	}

}
