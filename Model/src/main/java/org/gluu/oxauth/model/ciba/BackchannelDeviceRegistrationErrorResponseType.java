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
public enum BackchannelDeviceRegistrationErrorResponseType implements IErrorType {

    // HTTP 400 Bad Request

    /**
     * The request is missing a required parameter, includes an invalid parameter value,
     * includes a parameter more than once, or is otherwise malformed.
     */
    INVALID_REQUEST("invalid_request"),

    /**
     * The OpenID Provider is not able to identify the end-user.
     */
    UNKNOWN_USER_ID("unknown_user_id"),

    /**
     * The Client is not authorized to use this authentication flow.
     */
    UNAUTHORIZED_CLIENT("unauthorized_client"),

    // HTTP 403 Forbidden

    /**
     * The resource owner or OpenID Provider denied the request.
     */
    ACCESS_DENIED("access_denied");

    private final String paramName;

    BackchannelDeviceRegistrationErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link BackchannelDeviceRegistrationErrorResponseType} from a given string.
     *
     * @param param The string value to convert.
     * @return The corresponding {@link BackchannelDeviceRegistrationErrorResponseType}, otherwise <code>null</code>.
     */
    public static BackchannelDeviceRegistrationErrorResponseType fromString(String param) {
        if (param != null) {
            for (BackchannelDeviceRegistrationErrorResponseType err : BackchannelDeviceRegistrationErrorResponseType.values()) {
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