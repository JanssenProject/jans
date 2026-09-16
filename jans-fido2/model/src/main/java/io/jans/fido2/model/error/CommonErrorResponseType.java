package io.jans.fido2.model.error;

import io.jans.as.model.error.IErrorType;

public enum CommonErrorResponseType implements IErrorType {

    /**
     * The request is missing a required parameter, includes an
     * invalid parameter value, includes a parameter more than
     * once, or is otherwise malformed.
     */
    INVALID_REQUEST("invalid_request"),

    /**
     * The request contains invalid domain or don't match
     */
    INVALID_DOMAIN("invalid_domain"),

    /**
     * The ceremony was performed in a cross-origin iframe, which is not allowed.
     */
    CROSS_ORIGIN_NOT_ALLOWED("cross_origin_not_allowed"),

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
