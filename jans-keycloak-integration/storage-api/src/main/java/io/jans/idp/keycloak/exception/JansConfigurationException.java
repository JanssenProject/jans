/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.idp.keycloak.exception;

public class JansConfigurationException extends RuntimeException {


    public JansConfigurationException() {
    }

    public JansConfigurationException(String message) {
        super(message);
    }

    public JansConfigurationException(Throwable cause) {
        super(cause);
    }

    public JansConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
