/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.couchbase.exception;

/**
 * Exception for case when there are errors during user password validation
 *
 * @author Yuriy Movchan Date: 05/15/2018
 */
public class AuthenticationException extends RuntimeException {

    private static final long serialVersionUID = 1434816743469359856L;

    public AuthenticationException(String message) {
        super(message);
    }

}
