/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.couchbase.exception;

/**
 * Exception for case when it's not possible to convert DN to application key.
 *
 * @author Yuriy Movchan Date: 05/10/2018
 */
public class KeyConvertationException extends RuntimeException {

    private static final long serialVersionUID = 1234816743469359856L;

    public KeyConvertationException(String message) {
        super(message);
    }

}
