/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.exception;

/**
 * @author Yuriy Movchan
 * @version 0.1, 08/02/2013
 */
public class OxIntializationException extends Exception {

    private static final long serialVersionUID = 2148886143372789053L;

    public OxIntializationException(String message) {
        super(message);
    }

    public OxIntializationException(String message, Throwable cause) {
        super(message, cause);
    }

}
