/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.util.exception;

/**
 * This class represents the error when resource is not available or not exists
 *
 * @author Yuriy Movchan 07/12/2024
 */
public class MissingResourceException extends RuntimeException {

    private static final long serialVersionUID = 558936139180336683L;

    public MissingResourceException(final String message) {
        super(message);
    }

    public MissingResourceException(final Throwable cause) {
        super(cause);
    }

    public MissingResourceException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
