/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.ciba;

import org.gluu.oxauth.model.error.IErrorType;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public enum BackchannelAuthenticationErrorResponseType implements IErrorType {

    // HTTP 400 Bad Request

    /**
     * The request is missing a required parameter, includes an invalid parameter
     * value, includes a parameter more than once, contains more than one of the
     * hints, or is otherwise malformed.
     */
    INVALID_REQUEST("invalid_request"),

    /**
     * The requested scope is invalid, unknown, or malformed.
     */
    INVALID_SCOPE("invalid_scope"),

    /**
     * The login_hint_token provided in the authentication request is not valid because
     * it has expired.
     */
    EXPIRED_LOGIN_HINT_TOKEN("expired_login_hint_token"),

    /**
     * The OpenID Provider is not able to identify which end-user the Client wishes to
     * be authenticated by means of the hint provided in the request (login_hint_token,
     * id_token_hint or login_hint).
     */
    UNKNOWN_USER_ID("unknown_user_id"),

    /**
     * The Client is not authorized to use this authentication flow.
     */
    UNAUTHORIZED_CLIENT("unauthorized_client"),

    /**
     * User code is required but was missing from the request.
     */
    MISSING_USER_CODE("missing_user_code"),

    /**
     * User code was invalid.
     */
    INVALID_USER_CODE("invalid_user_code"),

    /**
     * The binding message is invalid or unacceptable for use in the context of the
     * given request.
     */
    INVALID_BINDING_MESSAGE("invalid_binding_message"),

    // HTTP 401 Unauthorized

    /**
     * Client authentication failed (e.g., invalid client credentials, unknown client, no
     * client authentication included, or unsupported authentication method).
     */
    INVALID_CLIENT("invalid_client"),

    /**
     * The end-user has not registered a device to receive push notifications.
     */
    UNAUTHORIZED_END_USER_DEVICE("unauthorized_end_user_device"),

    // HTTP 403 Forbidden

    /**
     * The resource owner or OpenID Provider denied the CIBA (Client Initiated
     * Backchannel Authentication) request.
     */
    ACCESS_DENIED("access_denied");

    private final String paramName;

    BackchannelAuthenticationErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link BackchannelAuthenticationErrorResponseType} from a given string.
     *
     * @param param The string value to convert.
     * @return The corresponding {@link BackchannelAuthenticationErrorResponseType}, otherwise <code>null</code>.
     */
    public static BackchannelAuthenticationErrorResponseType fromString(String param) {
        if (param != null) {
            for (BackchannelAuthenticationErrorResponseType err : BackchannelAuthenticationErrorResponseType.values()) {
                if (param.equals(err.paramName)) {
                    return err;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the object. In this case the parameter name.
     *
     * @return The string representation of the object.
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