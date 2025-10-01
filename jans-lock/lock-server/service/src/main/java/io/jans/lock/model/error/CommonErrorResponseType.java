package io.jans.lock.model.error;

import io.jans.as.model.error.IErrorType;

/**
 * @author Yuriy Movchan Date: 23/02/2024
 */
public enum CommonErrorResponseType implements IErrorType {

    /**
     * The request is missing a required parameter, includes an
     * invalid parameter value, includes a parameter more than
     * once, or is otherwise malformed.
     */
    INVALID_REQUEST("invalid_request"),
    
    /**
     * Unknown or not found error.
     */
    UNKNOWN_ERROR("unknown_error"),
    ;

    private final String paramName;

    CommonErrorResponseType(String paramName) {
        this.paramName = paramName;
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
