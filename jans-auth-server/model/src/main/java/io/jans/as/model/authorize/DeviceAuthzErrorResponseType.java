/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.authorize;

import io.jans.as.model.error.IErrorType;

/**
 * Error codes for device authz error responses.
 */
public enum DeviceAuthzErrorResponseType implements IErrorType {

    INVALID_CLIENT("invalid_client"),

    INVALID_GRANT("invalid_grant"),
    ;

    private final String paramName;

    DeviceAuthzErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Return the corresponding enumeration from a string parameter.
     *
     * @param param The parameter to be match.
     * @return The <code>enumeration</code> if found, otherwise
     * <code>null</code>.
     */
    public static DeviceAuthzErrorResponseType fromString(String param) {
        if (param != null) {
            for (DeviceAuthzErrorResponseType err : DeviceAuthzErrorResponseType
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
