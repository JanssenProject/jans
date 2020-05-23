package org.gluu.oxauthconfigapi.exception;

import java.io.Serializable;

public class ApiException extends Exception implements Serializable {
	private static final long serialVersionUID = 1L;

	public ApiException() {
		super();
	}

	public ApiException(String msg) {
		super(msg);
	}

	public ApiException(String msg, Exception e) {
		super(msg, e);
	}
}
