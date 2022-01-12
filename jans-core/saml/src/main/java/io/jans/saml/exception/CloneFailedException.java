/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.saml.exception;

public class CloneFailedException extends RuntimeException {

    private static final long serialVersionUID = 20091223L;

    public CloneFailedException(final String message) {
        super(message);
    }

    public CloneFailedException(final Throwable cause) {
        super(cause);
    }

    public CloneFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
