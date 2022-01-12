/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.exception;

/**
 * Configuration exception
 *
 * @author Yuriy Movchan
 * @version 0.1, 05/16/2013
 */
public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = -7590161991536595499L;

    public ConfigurationException() {
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
