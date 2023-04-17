/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f.error;

import io.jans.as.model.error.IErrorType;

/**
 * Error codes for fido2 error responses.
 *
 */
public enum Fido2ErrorResponseType implements IErrorType {

    /**
     * The request is missing a required parameter, includes an
     * invalid parameter value or is otherwise malformed id_session.
     */
    INVALID_ID_SESSION("invalid_id_session"),

    /**
     *  The request is missing a required parameter, username or keyhandle
     */
    INVALID_USERNAME_OR_KEYHANDLE("invalid_username_or_keyhandle"),

    /**
     *  The request is missing a required parameter, username or keyhandle
     */
    BAD_REQUEST_INTERCEPTION("bad_request_interception");


    private final String paramName;

    Fido2ErrorResponseType(String paramName) {
        this.paramName = paramName;
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