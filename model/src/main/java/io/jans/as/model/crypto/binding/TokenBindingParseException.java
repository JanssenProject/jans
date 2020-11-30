/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.binding;

/**
 * @author Yuriy Zabrovarnyy
 */
public class TokenBindingParseException extends Exception {

    public TokenBindingParseException() {
    }

    public TokenBindingParseException(String message) {
        super(message);
    }

    public TokenBindingParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenBindingParseException(Throwable cause) {
        super(cause);
    }

    public TokenBindingParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
