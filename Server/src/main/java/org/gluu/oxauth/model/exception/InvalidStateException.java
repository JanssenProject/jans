package org.gluu.oxauth.model.exception;

/**
 * Runtime exception to stop code execution if something is not right
 *
 * @author Yuriy Movchan Date: 09/08//2016
 *
 */
public class InvalidStateException extends RuntimeException {

	private static final long serialVersionUID = 6256375601182225949L;

	public InvalidStateException() {
		super();
	}

	public InvalidStateException(String message) {
		super(message);
	}

}
