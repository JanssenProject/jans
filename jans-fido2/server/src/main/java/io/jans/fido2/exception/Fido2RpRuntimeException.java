/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.exception;

import io.jans.fido2.model.error.Fido2RPError;

/**
 * Class for Fido2RpRuntimeException
 *
 */
public class Fido2RpRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -518563205092295773L;

    private final String status;
    private final String errorMessage;

    public Fido2RpRuntimeException(String errorMessage) {
        super(errorMessage);
        this.status = "failed";
        this.errorMessage = errorMessage;
    }

    public Fido2RpRuntimeException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.status = "failed";
        this.errorMessage = errorMessage;
    }

    public Fido2RpRuntimeException(String status, String errorMessage) {
        super(errorMessage);
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public Fido2RpRuntimeException(String status, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public Fido2RPError getFormattedMessage() {
        return new Fido2RPError(status, errorMessage);
    }
}
