/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.exception;

public class SCIMException extends RuntimeException {

    private static final long serialVersionUID = 7916443182706462000L;

    public SCIMException(String message) {
        super(message);
    }
    
    public SCIMException(String message, Throwable e) {
        super(message, e);
    }
    
}
