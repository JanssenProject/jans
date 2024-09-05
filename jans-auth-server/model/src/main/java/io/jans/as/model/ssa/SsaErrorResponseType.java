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
     * The value of one of the SSA Metadata fields is invalid and the server has rejected this request.
     * Note that an Authorization Server MAY choose to substitute a valid value for any requested parameter
     * of a SSA's Metadata.
     */
    INVALID_SSA_METADATA("invalid_ssa_metadata"),

    /**
     * When creating a ssa, if you get an internal error.
     */
    UNKNOWN_ERROR("unknown_error"),

    /**
     * When the signature has expired or the algorithm for signing does not exist
     */
    INVALID_SIGNATURE("invalid_signature"),

    /**
     * When jti does not exist or is invalid
     */
    INVALID_JTI("invalid_jti"),
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
