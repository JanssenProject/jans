/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.lock.model.error;

import io.jans.as.model.error.IErrorType;

/**
 * @author Yuriy Movchan Date: 23/02/2024
 */
public enum StatErrorResponseType implements IErrorType {
    /**
     * The request is missing a required parameter, includes an unsupported
     * parameter or parameter value, repeats a parameter, includes multiple
     * credentials, utilizes more than one mechanism for authenticating the
     * client, or is otherwise malformed.
     */
    INVALID_REQUEST("invalid_request"),

    /**
     * The end-user denied the authorization request.
     */
    ACCESS_DENIED("access_denied");

    private final String paramName;

    StatErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link StatErrorResponseType} from a given string.
     *
     * @param param The string value to convert.
     * @return The corresponding {@link StatErrorResponseType}, otherwise <code>null</code>.
     */
    public static StatErrorResponseType fromString(String param) {
        if (param != null) {
            for (StatErrorResponseType err : StatErrorResponseType.values()) {
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
