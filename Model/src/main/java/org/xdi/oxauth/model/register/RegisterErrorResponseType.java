/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.register;

import org.xdi.oxauth.model.error.IErrorType;

/**
 * Error codes for register error responses.
 *
 * @author Javier Rojas Blum Date: 01.17.2012
 */
public enum RegisterErrorResponseType implements IErrorType {

    /**
     * The value of client_id is invalid.
     */
    INVALID_CLIENT_ID("invalid_client_id"),
    /**
     * Value of one or more redirect_uris is invalid.
     */
    INVALID_REDIRECT_URI("invalid_redirect_uri"),
    /**
     * The value of one of the configuration parameters is invalid.
     */
    INVALID_CONFIGURATION_PARAMETER("invalid_configuration_parameter"),
    /**
     * The request is missing a required parameter, includes an unsupported parameter or parameter value, repeats the
     * same parameter, uses more than one method for including an access token, or is otherwise malformed.
     */
    INVALID_REQUEST("invalid_request"),
    /**
     * The access token provided is expired, revoked, malformed, or invalid for other reasons.
     */
    INVALID_TOKEN("invalid_token"),
    /**
     * The authorization server denied the request.
     */
    ACCESS_DENIED("access_denied");

    private final String paramName;

    private RegisterErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Return the corresponding enumeration from a string parameter.
     *
     * @param param The parameter to be match.
     * @return The <code>enumeration</code> if found, otherwise
     *         <code>null</code>.
     */
    public static RegisterErrorResponseType fromString(String param) {
        if (param != null) {
            for (RegisterErrorResponseType err : RegisterErrorResponseType
                    .values()) {
                if (param.equals(err.paramName)) {
                    return err;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the object. In this case, the lower case code of the error.
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
