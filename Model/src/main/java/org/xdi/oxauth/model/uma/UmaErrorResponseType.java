/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import org.xdi.oxauth.model.error.IErrorType;

import java.util.HashMap;
import java.util.Map;

/**
 * Error codes for UMA error responses.
 * 
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * Date: 10.03.2012
 */
public enum UmaErrorResponseType implements IErrorType {

	/**
	 * The request is missing a required parameter, includes an unsupported
	 * parameter or parameter value, or is otherwise malformed.
	 */
	INVALID_REQUEST("invalid_request"),

	/**
	 * The client is not authorized to request an access token using this
	 * method.
	 */
	UNAUTHORIZED_CLIENT("unauthorized_client"),

	/**
	 * The resource owner or AM server denied the request.
	 */
	ACCESS_DENIED("access_denied"),

	/**
	 * The AM server does not support obtaining an access token using
	 * this method.
	 */
	UNSUPPORTED_RESPONSE_TYPE("unsupported_response_type"),

	/**
	 * The requested scope is invalid, unknown, or malformed.
	 */
	INVALID_CLIENT_SCOPE("invalid_client_scope"),

	/**
	 * The AM server encountered an unexpected condition which
	 * prevented it from fulfilling the request.
	 */
	SERVER_ERROR("server_error"),

	/**
	 * The AM server is currently unable to handle the request due to
	 * a temporary overloading or maintenance of the server.
	 */
	TEMPORARILY_UNAVAILABLE("temporarily_unavailable"),

	/**
	 * The resource set that was requested to be deleted or updated at the AM
	 * did not match the If-Match value present in the request.
	 */
	PRECONDITION_FAILED("precondition_failed"),

	/**
	 * The resource set requested from the AM cannot be found.
	 */
	NOT_FOUND("not_found"),

	/**
	 * The host request used an unsupported HTTP method.
	 */
	UNSUPPORTED_METHOD_TYPE("unsupported_method_type"),
	
	/**
	 * The access token expired.
	 */
	INVALID_TOKEN("invalid_token"),	

	/**
	 * The provided resource set identifier was not found at the AM.
	 */
	INVALID_RESOURCE_SET_ID("invalid_resource_set_id"),

	/**
	 * The provided resource set identifier was not found at the AM.
	 */
	INVALID_RESOURCE_SET_SCOPE("invalid_scope"),
	
	/**
	 * The provided ticket was not found at the AM.
	 */
	INVALID_TICKET("invalid_ticket"),
	
	/**
	 * The provided ticket has expired.
	 */
	EXPIRED_TICKET("expired_ticket"),
	
	/**
	 * The requester is definitively not authorized for this permission according to user policy.
	 */
	NOT_AUTHORIZED_PERMISSION("not_authorized_permission"),
	
	/**
	 * The AM is unable to determine whether the requester is authorized for this permission without gathering claims from the requesting party.
	 */
	NEED_CLAIMS("need_claims");

    private static Map<String, UmaErrorResponseType> lookup = new HashMap<String, UmaErrorResponseType>();

    static {
        for (UmaErrorResponseType enumType : values()) {
            lookup.put(enumType.getParameter(), enumType);
        }
    }

	private final String paramName;

	private UmaErrorResponseType(String paramName) {
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
	public static UmaErrorResponseType fromString(String param) {
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