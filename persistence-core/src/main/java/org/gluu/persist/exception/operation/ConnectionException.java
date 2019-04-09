/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.exception.operation;

/**
 * Configuration exception
 *
 * @author Yuriy Movchan
 * @version 0.1, 05/16/2013
 */
public class ConnectionException extends RuntimeException {

    private static final long serialVersionUID = -7590161991536595499L;

    public ConnectionException() {
    }

    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(Throwable cause) {
        super(cause);
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

}
