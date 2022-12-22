/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f.exception;

public class InvalidKeyHandleDeviceException extends Exception {

    private static final long serialVersionUID = 4324358428668365475L;

    public InvalidKeyHandleDeviceException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidKeyHandleDeviceException(String message) {
        super(message);
    }

}
