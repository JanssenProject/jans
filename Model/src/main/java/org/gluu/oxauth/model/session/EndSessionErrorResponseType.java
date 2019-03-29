/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.session;

import org.gluu.oxauth.model.error.IErrorType;

/**
 * Error codes for End Session error responses.
 *
 * @author Javier Rojas Blum Date: 12.16.2011
 */
public enum EndSessionErrorResponseType implements IErrorType {
    /**
     * The provided access token is invalid, or was issued to another client.
     */
    INVALID_GRANT("invalid_grant"),

    /**
     * The request is missing a required parameter, includes an unsupported parameter or parameter value, repeats a
     * parameter, or is otherwise malformed.
     */
    INVALID_REQUEST("invalid_request"),

    /**
     * The provided access token and session state are invalid or were issued to another client.
     */
    INVALID_GRANT_AND_SESSION("invalid_grant_and_session"),

    /**
     * The provided session state is empty.
     */
    SESSION_NOT_PASSED("session_not_passed"),

    /**
     * The provided post logout uri is empty.
     */
    POST_LOGOUT_URI_NOT_PASSED("post_logout_uri_not_passed"),

    /**
     * The provided post logout uri is not associated with client
     */
    POST_LOGOUT_URI_NOT_ASSOCIATED_WITH_CLIENT("post_logout_uri_not_associated_with_client");

    private final String paramName;

    private EndSessionErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link EndSessionErrorResponseType} from a given string.
     *
     * @param param The string value to convert.
     * @return The corresponding {@link EndSessionErrorResponseType}, otherwise <code>null</code>.
     */
    public static EndSessionErrorResponseType fromString(String param) {
        if (param != null) {
            for (EndSessionErrorResponseType err : EndSessionErrorResponseType
                    .values()) {
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