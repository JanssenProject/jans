/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.uma.exception;

/**
 * UMA Exception
 *
 * @author Yuriy Movchan Date: 12/08/2012
 */
public class UmaException extends Exception {

    private static final long serialVersionUID = 2136659058534678566L;

    public UmaException() {
    }

    public UmaException(String message) {
    }

    public UmaException(Throwable cause) {
        super(cause);
    }

    public UmaException(String message, Throwable cause) {
        super(message, cause);
    }

}
