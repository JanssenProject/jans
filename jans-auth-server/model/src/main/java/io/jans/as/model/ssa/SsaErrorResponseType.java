/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.ssa;

import io.jans.as.model.error.IErrorType;

public enum SsaErrorResponseType implements IErrorType {

    /**
     * If the client does not have the ssa.admin scope enabled.
     */
    UNAUTHORIZED_CLIENT("unauthorized_client"),

    /**
     * If there is no valid client in the request.
     */
    INVALID_CLIENT("invalid_client"),

    /**
     * When creating an ssa, if you get an internal error.
     */
    UNKNOWN_ERROR("unknown_error"),
    ;

    private final String paramName;

    SsaErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    public static SsaErrorResponseType fromString(String param) {
        if (param != null) {
            for (SsaErrorResponseType err : SsaErrorResponseType
                    .values()) {
                if (param.equals(err.paramName)) {
                    return err;
                }
            }
        }
        return null;
    }

    @Override
    public String getParameter() {
        return paramName;
    }

    @Override
    public String toString() {
        return paramName;
    }
}
