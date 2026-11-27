/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.model.attestation;

import io.jans.as.model.error.IErrorType;

/**
 * Error codes of the attestation endpoint. A negative verdict is never an error: it is a 200 response.
 */
public enum AttestationErrorResponseType implements IErrorType {

    INVALID_REQUEST("invalid_request"),
    INVALID_CLIENT("invalid_client"),
    KEY_NOT_REGISTERED("key_not_registered"),
    INVALID_CHALLENGE("invalid_challenge"),
    UNSUPPORTED_EVIDENCE_FORMAT("unsupported_evidence_format"),
    EVIDENCE_TOO_LARGE("evidence_too_large"),
    VERIFIER_UNAVAILABLE("verifier_unavailable"),
    ;

    private final String paramName;

    AttestationErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    public static AttestationErrorResponseType fromString(String param) {
        if (param != null) {
            for (AttestationErrorResponseType err : values()) {
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
