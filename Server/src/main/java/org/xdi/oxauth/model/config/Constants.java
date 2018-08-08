/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.config;

/**
 * Constants
 * 
 * @author Yuriy Movchan Date: 10.14.2010
 */
public final class Constants {

	public static final String RESULT_SUCCESS = "success";
	public static final String RESULT_FAILURE = "failure";
	public static final String RESULT_DUPLICATE = "duplicate";
	public static final String RESULT_DISABLED = "disabled";
	public static final String RESULT_NO_PERMISSIONS = "no_permissions";
	public static final String RESULT_INVALID_STEP = "invalid_step";
	public static final String RESULT_VALIDATION_ERROR = "validation_error";
	public static final String RESULT_LOGOUT = "logout";
	public static final String RESULT_EXPIRED = "expired";

	public static final String U2F_PROTOCOL_VERSION = "U2F_V2";
	
	public static final String OX_AUTH_SCOPE_TYPE_OPENID = "openid";

	public static final String REMOTE_IP = "remote_ip";
	public static final String AUTHENTICATED_USER = "auth_user";

	public static final boolean SKIP_CACHE_PUT_FOR_NATIVE_PERSISTENCE = true;

}
