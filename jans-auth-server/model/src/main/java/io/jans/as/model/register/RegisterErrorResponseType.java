/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.register;

import io.jans.as.model.error.IErrorType;

/**
 * Error codes for register error responses.
 *
 * @author Javier Rojas Blum
 * @version February 10, 2022
 */
public enum RegisterErrorResponseType implements IErrorType {

    /**
     * Value of one or more redirect_uris is invalid.
     */
    INVALID_REDIRECT_URI("invalid_redirect_uri"),

    /**
     * Value of one or more claims_redirect_uris is invalid.
     */
    INVALID_CLAIMS_REDIRECT_URI("invalid_claims_redirect_uri"),

    /**
     * The value of one of the Client Metadata fields is invalid and the server has rejected this request.
     * Note that an Authorization Server MAY choose to substitute a valid value for any requested parameter
     * of a Client's Metadata.
     */
    INVALID_CLIENT_METADATA("invalid_client_metadata"),
    /**
     * The access token provided is expired, revoked, malformed, or invalid for other reasons.
     */
    INVALID_TOKEN("invalid_token"),

    /**
     * Value of logout_uri is invalid.
     */
    INVALID_LOGOUT_URI("invalid_logout_uri"),

    /**
     * Invalid software statement.
     */
    INVALID_SOFTWARE_STATEMENT("invalid_software_statement"),

    /**
     * The authorization server denied the request.
     */
    ACCESS_DENIED("access_denied"),

    INVALID_PUBLIC_SUBJECT_IDENTIFIER_ATTRIBUTE("invalid_public_subject_identifier_attribute"),

    INVALID_REDIRECT_URIS_REGEX("invalid_redirect_uris_regex");

    private final String paramName;

    RegisterErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Return the corresponding enumeration from a string parameter.
     *
     * @param param The parameter to be match.
     * @return The <code>enumeration</code> if found, otherwise
     * <code>null</code>.
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
