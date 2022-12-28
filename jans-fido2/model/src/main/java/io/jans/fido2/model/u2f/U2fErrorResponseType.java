/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package io.jans.fido2.model.u2f;


import io.jans.as.model.error.IErrorType;

import java.util.HashMap;
import java.util.Map;

/**
 * Error codes for FIDO U2F server
 * 
 * @author Yuriy Movchan Date: 05/13/2015
 */
public enum U2fErrorResponseType implements IErrorType {

	/**
	 * The FIDO U2F server encountered an unexpected condition which prevented
	 * it from fulfilling the request.
	 */
	SERVER_ERROR("server_error"),
	
	/**
	 * The authentication or registration request contains invalid data or signature.
	 */
	INVALID_REQUEST("invalid_request"),

	/**
	 * The user has no registered devices needed to build authentication request.
	 */
	NO_ELIGABLE_DEVICES("no_eligable_devices"),

	/**
	 * The registered device was compromised.
	 */
	DEVICE_COMPROMISED("device_compromised"),

	/**
	 * The authentication or registration session was expired
	 */
	SESSION_EXPIRED("session_expired"),

	/**
	 * The user has registered device already.
	 */
	REGISTRATION_NOT_ALLOWED("registration_not_allowed");
	

	private static Map<String, U2fErrorResponseType> lookup = new HashMap<String, U2fErrorResponseType>();

	static {
		for (U2fErrorResponseType enumType : values()) {
			lookup.put(enumType.getParameter(), enumType);
		}
	}

	private final String paramName;

	private U2fErrorResponseType(String paramName) {
		this.paramName = paramName;
	}

	/**
	 * Return the corresponding enumeration from a string parameter.
	 * 
	 * @param param
	 *            The parameter to be match.
	 * @return The <code>enumeration</code> if found, otherwise
	 *         <code>null</code>.
	 */
	public static U2fErrorResponseType fromString(String param) {
		return lookup.get(param);
	}

	/**
	 * Returns a string representation of the object. In this case, the lower
	 * case code of the error.
	 */
	@Override
	public String toString() {
		return paramName;
	}

	/**
	 * Gets error parameter.
	 * 
	 * @return error parameter
	 */
	@Override
	public String getParameter() {
		return paramName;
	}
}