/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.uma;

import io.jans.as.model.error.IErrorType;

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
	 * The client is disabled and can't request an access token using this method.
	 */
	DISABLED_CLIENT("disabled_client"),

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
	 * Forbidden by policy (policy returned false).
	 */
	FORBIDDEN_BY_POLICY("forbidden_by_policy"),
	
	/**
	 * The access token expired.
	 */
	INVALID_TOKEN("invalid_token"),

	/**
	 * Grant type is not urn:ietf:params:oauth:grant-type:uma-ticket (required for UMA 2).
	 */
	INVALID_GRANT_TYPE("invalid_grant_type"),

	/**
	 * Invalid permission request.
	 */
	INVALID_PERMISSION_REQUEST("invalid_permission_request"),

	/**
	 * The provided resource id was not found at the AS.
	 */
	INVALID_RESOURCE_ID("invalid_resource_id"),

	/**
	 * At least one of the scopes included in the request was not registered previously by this host.
	 */
	INVALID_SCOPE("invalid_scope"),

	/**
	 * The provided client_id is not valid.
	 */
	INVALID_CLIENT_ID("invalid_client_id"),

	/**
	 * The provided invalid_claims_redirect_uri is not valid.
	 */
	INVALID_CLAIMS_REDIRECT_URI("invalid_claims_redirect_uri"),

	/**
	 * The provided ticket was not found at the AS.
	 */
	INVALID_TICKET("invalid_ticket"),

	/**
	 * The claims-gathering script name is not provided or otherwise failed to load script with this name(s).
	 */
	INVALID_CLAIMS_GATHERING_SCRIPT_NAME("invalid_claims_gathering_script_name"),

	/**
	 * The provided ticket has expired.
	 */
	EXPIRED_TICKET("expired_ticket"),

	/**
	 * The provided session is invalid.
	 */
	INVALID_SESSION("invalid_session"),

	/**
	 * The claim token format is blank or otherwise not supported (supported format is http://openid.net/specs/openid-connect-core-1_0.html#IDToken).
	 */
	INVALID_CLAIM_TOKEN_FORMAT("invalid_claim_token_format"),

	/**
	 * The claim token is not valid or unsupported. (If format is http://openid.net/specs/openid-connect-core-1_0.html#IDToken then claim token has to be ID Token).
	 */
	INVALID_CLAIM_TOKEN("invalid_claim_token"),

	/**
	 * PCT is invalid (revoked, expired or does not exist anymore on AS)
	 */
	INVALID_PCT("invalid_pct"),

	/**
	 * RPT is invalid (revoked, expired or does not exist anymore on AS)
	 */
	INVALID_RPT("invalid_rpt"),

	/**
	 * The requester is definitively not authorized for this permission according to user policy.
	 */
	NOT_AUTHORIZED_PERMISSION("not_authorized_permission"),
	
	/**
	 * The AM is unable to determine whether the requester is authorized for this permission without gathering claims from the requesting party.
	 */
	NEED_CLAIMS("need_claims");

    private static Map<String, UmaErrorResponseType> lookup = new HashMap<>();

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