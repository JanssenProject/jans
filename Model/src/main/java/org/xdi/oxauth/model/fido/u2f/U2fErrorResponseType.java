/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.xdi.oxauth.model.fido.u2f;

import java.util.HashMap;
import java.util.Map;

import org.xdi.oxauth.model.error.IErrorType;

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
	 * The FIDO U2F server is currently unable to handle the request due to a
	 * temporary overloading or maintenance of the server.
	 */
	TEMPORARILY_UNAVAILABLE("temporarily_unavailable");

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