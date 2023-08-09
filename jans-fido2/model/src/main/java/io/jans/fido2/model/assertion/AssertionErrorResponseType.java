package io.jans.fido2.model.assertion;

import io.jans.as.model.error.IErrorType;

public enum AssertionErrorResponseType implements IErrorType {

    /**
     * The request is missing a required parameter, includes an
     * invalid parameter value or is otherwise malformed id_session.
     */
    INVALID_SESSION_ID("invalid_session_id"),

    /**
     *  The request is missing a required parameter, username or key_handle
     */
    INVALID_USERNAME_OR_KEY_HANDLE("invalid_username_or_key_handle"),

    /**
     * The request contains an unsupported authentication type
     */
    UNSUPPORTED_AUTHENTICATION_TYPE("unsupported_authentication_type"),

    /**
     * The request contains conflicts with Super Gluu parameters
     */
    CONFLICT_WITH_SUPER_GLUU("conflict_with_super_gluu"),

    /**
     * Can't find associated key(s)
     */
    KEYS_NOT_FOUND("keys_not_found"),
    ;

    private final String paramName;

    AssertionErrorResponseType(String paramName) {
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
