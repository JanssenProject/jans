/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.exception;

/**
 * @author Javier Rojas Blum Date: 12.06.2012
 */
public class InvalidJweException extends Exception {

    public InvalidJweException(String message) {
        super(message);
    }

    public InvalidJweException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidJweException(Throwable cause) {
        super(cause);
    }
}