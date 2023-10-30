/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.kc.idp.broker.exception;

public class JansIdpBrokerException extends RuntimeException {


    public JansIdpBrokerException() {
    }

    public JansIdpBrokerException(String message) {
        super(message);
    }

    public JansIdpBrokerException(Throwable cause) {
        super(cause);
    }

    public JansIdpBrokerException(String message, Throwable cause) {
        super(message, cause);
    }

}
