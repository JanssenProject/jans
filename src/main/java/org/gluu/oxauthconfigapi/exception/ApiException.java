package org.gluu.oxauthconfigapi.exception;

import java.io.Serializable;

public class ApiException extends Exception implements Serializable {
	private static final long serialVersionUID = 1L;

	private ApiExceptionType type = ApiExceptionType.DEFAULT;

	private String subject;

	public ApiException() {
		super();
	}

	public ApiException(ApiExceptionType type, String subject) {
		super();
		this.setType(type);
		this.setSubject(subject);
	}

	public ApiException(ApiExceptionType type) {
		super();
		this.setType(type);
	}

	public ApiExceptionType getType() {
		return type;
	}

	private void setType(ApiExceptionType type) {
		this.type = type;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

}
