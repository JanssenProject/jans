/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f.exception;

/**
 * @author Yuriy Movchan Date: 07/13/2016
 */
public class RegistrationNotAllowed extends RuntimeException {

    private static final long serialVersionUID = -2738024707341148557L;

    public RegistrationNotAllowed(String message) {
        super(message);
    }

    public RegistrationNotAllowed(String message, Throwable cause) {
        super(message, cause);
    }
}
