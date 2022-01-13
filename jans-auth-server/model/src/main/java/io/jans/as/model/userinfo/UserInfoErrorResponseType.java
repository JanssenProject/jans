/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.userinfo;

import io.jans.as.model.error.IErrorType;

/**
 * @author Javier Rojas Date: 11.30.2011
 */
public enum UserInfoErrorResponseType implements IErrorType {

    /**
     * The request is missing a required parameter, includes an unsupported parameter or parameter value, repeats
     * the same parameter, uses more than one method for including an access token, or is otherwise malformed.
     */
    INVALID_REQUEST("invalid_request"),
    /**
     * The access token provided is expired, revoked, malformed, or invalid for other reasons. Try to request a
     * new access token and retry the protected resource.
     */
    INVALID_TOKEN("invalid_token"),
    /**
     * The request requires higher privileges than provided by the access token.
     */
    INSUFFICIENT_SCOPE("insufficient_scope");

    private final String paramName;

    UserInfoErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    public static UserInfoErrorResponseType fromString(String param) {
        if (param != null) {
            for (UserInfoErrorResponseType err : UserInfoErrorResponseType.values()) {
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

    @Override
    public String getParameter() {
        return paramName;
    }
}